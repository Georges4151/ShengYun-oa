package com.ceams.module.doc.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.common.response.PageResult;
import com.ceams.common.security.SecurityUtils;
import com.ceams.module.bpm.enums.AuditAction;
import com.ceams.module.bpm.service.BpmAuditService;
import com.ceams.module.bpm.support.BpmSecurityGuard;
import com.ceams.module.doc.dto.FileMoveRequest;
import com.ceams.module.doc.dto.FileRenameRequest;
import com.ceams.module.doc.entity.DocDirectory;
import com.ceams.module.doc.entity.DocFile;
import com.ceams.module.doc.enums.DocAction;
import com.ceams.module.doc.mapper.DocDirectoryMapper;
import com.ceams.module.doc.mapper.DocFileMapper;
import com.ceams.module.doc.service.DocFileService;
import com.ceams.module.doc.service.DocLockService;
import com.ceams.module.doc.support.DocAccessResolver;
import com.ceams.module.doc.support.DocConverter;
import com.ceams.module.doc.vo.DocFileVO;
import com.ceams.module.hr.util.HrPageUtil;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 文档文件服务实现（P2 仅元数据）。
 *
 * <p>重命名拒绝含路径分隔符、Windows 非法字符与控制字符的名称；移动要求目标目录与源目录同库；
 * 删除为软删除并写 {@code DOC_DELETE} 审计，重命名与移动写 {@code DOC_EDIT} 审计。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocFileServiceImpl implements DocFileService {

    /** 审计目标类型。 */
    private static final String AUDIT_TARGET = "DOC_FILE";

    /** 文件编辑权限码（V39 种子）。 */
    private static final String PERMISSION_FILE_EDIT = "doc:file:edit";

    /** 文件删除权限码（V39 种子）。 */
    private static final String PERMISSION_FILE_DELETE = "doc:file:delete";

    /** 文件名非法字符（含路径分隔符）。 */
    private static final String ILLEGAL_NAME_CHARS = "/\\:*?\"<>|";

    /** 文件名最大长度。 */
    private static final int MAX_NAME_LENGTH = 255;

    private final DocFileMapper fileMapper;
    private final DocDirectoryMapper dirMapper;
    private final DocAccessResolver accessResolver;
    private final BpmSecurityGuard securityGuard;
    private final BpmAuditService bpmAuditService;
    private final DocLockService docLockService;

    @Override
    public PageResult<DocFileVO> list(Long dirId, String keyword, int page, int size) {
        if (dirId == null) {
            throw BusinessException.of(ErrorCode.VALID_REQUIRED_FIELD, "目录 ID 不能为空");
        }
        DocDirectory dir = requireDir(dirId);
        accessResolver.require(DocAction.PREVIEW, dir, null);

        Page<DocFile> p = HrPageUtil.resolve(page, size);
        LambdaQueryWrapper<DocFile> wrapper =
                new LambdaQueryWrapper<DocFile>().eq(DocFile::getDirId, dirId);
        if (StringUtils.isNotBlank(keyword)) {
            wrapper.like(DocFile::getName, keyword.trim());
        }
        wrapper.orderByDesc(DocFile::getUpdateTime).orderByDesc(DocFile::getId);
        IPage<DocFile> result = fileMapper.selectPage(p, wrapper);
        List<DocFileVO> records = result.getRecords().stream().map(DocConverter::toFileVO).toList();
        return new PageResult<>(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rename(Long id, FileRenameRequest request) {
        DocFile file = requireFile(id);
        DocDirectory dir = requireDir(file.getDirId());
        requireAction(PERMISSION_FILE_EDIT, DocAction.EDIT, dir, file);

        String name = normalizeFileName(request.getName());
        if (!name.equals(file.getName())) {
            ensureNameUnique(name, dir.getId(), id);
        }
        String beforeName = file.getName();
        file.setName(name);
        file.setExt(extOf(name));
        fileMapper.updateById(file);

        Map<String, Object> before = new LinkedHashMap<>();
        before.put("name", beforeName);
        Map<String, Object> after = new LinkedHashMap<>();
        after.put("name", name);
        recordAudit(id, AuditAction.DOC_EDIT, before, after);
        log.info("文档文件 {} 重命名：{} -> {}", id, beforeName, name);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void move(Long id, FileMoveRequest request) {
        DocFile file = requireFile(id);
        DocDirectory sourceDir = requireDir(file.getDirId());
        requireAction(PERMISSION_FILE_EDIT, DocAction.EDIT, sourceDir, file);

        Long targetDirId = request.getDirId();
        DocDirectory targetDir = requireDir(targetDirId);
        if (!sameLibrary(sourceDir, targetDir)) {
            throw BusinessException.of(ErrorCode.VALID_ILLEGAL_ARGUMENT, "不允许跨库移动文件");
        }
        if (!Objects.equals(targetDirId, file.getDirId())) {
            ensureNameUnique(file.getName(), targetDirId, id);
        }
        Long beforeDirId = file.getDirId();
        file.setDirId(targetDirId);
        fileMapper.updateById(file);

        Map<String, Object> before = new LinkedHashMap<>();
        before.put("dirId", beforeDirId);
        Map<String, Object> after = new LinkedHashMap<>();
        after.put("dirId", targetDirId);
        recordAudit(id, AuditAction.DOC_EDIT, before, after);
        log.info("文档文件 {} 移动：dirId {} -> {}", id, beforeDirId, targetDirId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        DocFile file = requireFile(id);
        DocDirectory dir = requireDir(file.getDirId());
        requireAction(PERMISSION_FILE_DELETE, DocAction.DELETE, dir, file);

        Map<String, Object> before = snapshot(file);
        // 软删除前先释放编辑锁：幂等清理 Redis 锁 key 与数据库锁定标记（status=NORMAL、lock_user_id/lock_time 置空）
        docLockService.forceUnlock(id);
        // 逻辑删除：MyBatis-Plus @TableLogic 自动置 deleted=1
        fileMapper.deleteById(id);
        recordAudit(id, AuditAction.DOC_DELETE, before, null);
        log.info("文档文件 {} 已删除：name={}", id, file.getName());
    }

    // ------------------------------------------------------------------
    // 内部工具
    // ------------------------------------------------------------------

    private DocFile requireFile(Long id) {
        DocFile file = id == null ? null : fileMapper.selectById(id);
        if (file == null) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "文件不存在");
        }
        return file;
    }

    private DocDirectory requireDir(Long id) {
        DocDirectory dir = id == null ? null : dirMapper.selectById(id);
        if (dir == null) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "目录不存在");
        }
        return dir;
    }

    /** 权限：命中权限码直接放行（管理员由守卫放行），否则按数据权限校验。 */
    private void requireAction(
            String permissionCode, DocAction action, DocDirectory dir, DocFile file) {
        if (securityGuard.hasPermission(permissionCode)) {
            return;
        }
        accessResolver.require(action, dir, file);
    }

    /**
     * 文件名归一化：去首尾空白，拒绝空名、超长、路径分隔符、Windows 非法字符与控制字符。
     *
     * <p>重命名不做路径裁剪，含分隔符一律拒绝，避免前端路径污染文件名。
     */
    private String normalizeFileName(String raw) {
        String name = StringUtils.trimToNull(raw);
        if (name == null) {
            throw BusinessException.of(ErrorCode.VALID_REQUIRED_FIELD, "文件名不能为空");
        }
        if (name.length() > MAX_NAME_LENGTH) {
            throw BusinessException.of(
                    ErrorCode.VALID_ILLEGAL_ARGUMENT, "文件名不能超过 " + MAX_NAME_LENGTH + " 个字符");
        }
        if (name.contains("..")) {
            throw BusinessException.of(ErrorCode.VALID_ILLEGAL_ARGUMENT, "文件名不能包含 ..");
        }
        for (int i = 0; i < name.length(); i++) {
            char ch = name.charAt(i);
            if (ch < 0x20 || ch == 0x7F || ILLEGAL_NAME_CHARS.indexOf(ch) >= 0) {
                throw BusinessException.of(
                        ErrorCode.VALID_ILLEGAL_ARGUMENT, "文件名包含非法字符：" + name);
            }
        }
        return name;
    }

    /** 取小写扩展名，无扩展名返回 null。 */
    private String extOf(String name) {
        int dot = name.lastIndexOf('.');
        if (dot <= 0 || dot == name.length() - 1) {
            return null;
        }
        return name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    /** 同目录内文件名唯一（deleted=0 范围内）。 */
    private void ensureNameUnique(String name, Long dirId, Long excludeFileId) {
        LambdaQueryWrapper<DocFile> wrapper =
                new LambdaQueryWrapper<DocFile>()
                        .eq(DocFile::getDirId, dirId)
                        .eq(DocFile::getName, name);
        if (excludeFileId != null) {
            wrapper.ne(DocFile::getId, excludeFileId);
        }
        Long count = fileMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw BusinessException.of(
                    ErrorCode.VALID_ILLEGAL_ARGUMENT, "同目录下已存在同名文件：" + name);
        }
    }

    /** 同库判定：库类型与三个 owner 字段必须完全一致。 */
    private boolean sameLibrary(DocDirectory left, DocDirectory right) {
        return left.getLibraryType() == right.getLibraryType()
                && Objects.equals(left.getOwnerUserId(), right.getOwnerUserId())
                && Objects.equals(left.getOwnerDeptId(), right.getOwnerDeptId())
                && Objects.equals(left.getProjectId(), right.getProjectId());
    }

    private Map<String, Object> snapshot(DocFile file) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("name", file.getName());
        snapshot.put("dirId", file.getDirId());
        snapshot.put("currentVersionNo", file.getCurrentVersionNo());
        return snapshot;
    }

    private void recordAudit(
            Long targetId, AuditAction action, Object before, Object after) {
        bpmAuditService.record(
                AUDIT_TARGET,
                targetId,
                action,
                before,
                after,
                null,
                SecurityUtils.getCurrentUserIdOrDefault());
    }
}
