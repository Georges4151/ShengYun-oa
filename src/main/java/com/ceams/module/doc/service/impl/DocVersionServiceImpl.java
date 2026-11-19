package com.ceams.module.doc.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.common.security.SecurityUtils;
import com.ceams.module.bpm.enums.AuditAction;
import com.ceams.module.bpm.service.BpmAuditService;
import com.ceams.module.bpm.support.BpmSecurityGuard;
import com.ceams.module.doc.entity.DocDirectory;
import com.ceams.module.doc.entity.DocFile;
import com.ceams.module.doc.entity.DocFileVersion;
import com.ceams.module.doc.enums.DocAction;
import com.ceams.module.doc.enums.DocVersionSource;
import com.ceams.module.doc.mapper.DocDirectoryMapper;
import com.ceams.module.doc.mapper.DocFileMapper;
import com.ceams.module.doc.mapper.DocFileVersionMapper;
import com.ceams.module.doc.service.DocIndexService;
import com.ceams.module.doc.service.DocVersionService;
import com.ceams.module.doc.support.DocAccessResolver;
import com.ceams.module.doc.support.DocConverter;
import com.ceams.module.doc.support.DocStorageService;
import com.ceams.module.doc.vo.DocFileVO;
import com.ceams.module.doc.vo.DocVersionVO;
import com.ceams.module.iam.entity.SysUser;
import com.ceams.module.iam.mapper.UserMapper;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 文档版本服务实现。
 *
 * <p>回滚不删除历史：读取目标版本物理文件内容后写入新的存储 key，生成 {@code current+1} 版本
 * （{@code source=ROLLBACK}，{@code change_note=回滚到 vN}），唯一键冲突最多重试 3 次。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocVersionServiceImpl implements DocVersionService {

    /** 审计目标类型。 */
    private static final String AUDIT_TARGET = "DOC_FILE";

    /** 编辑权限码（V39 种子）。 */
    private static final String PERMISSION_FILE_EDIT = "doc:file:edit";

    /** 版本号冲突最大重试次数。 */
    private static final int MAX_VERSION_RETRY = 3;

    private final DocFileMapper fileMapper;
    private final DocDirectoryMapper dirMapper;
    private final DocFileVersionMapper versionMapper;
    private final DocAccessResolver accessResolver;
    private final BpmSecurityGuard securityGuard;
    private final BpmAuditService bpmAuditService;
    private final DocStorageService storageService;
    private final DocIndexService docIndexService;
    private final UserMapper userMapper;

    @Override
    public List<DocVersionVO> list(Long fileId) {
        DocFile file = requireFile(fileId);
        DocDirectory dir = requireDir(file.getDirId());
        accessResolver.require(DocAction.PREVIEW, dir, file);

        List<DocFileVersion> versions =
                versionMapper.selectList(
                        new LambdaQueryWrapper<DocFileVersion>()
                                .eq(DocFileVersion::getFileId, fileId)
                                .orderByDesc(DocFileVersion::getVersionNo));
        if (versions == null) {
            return List.of();
        }
        List<DocVersionVO> vos =
                versions.stream()
                        .filter(Objects::nonNull)
                        .map(DocConverter::toVersionVO)
                        .filter(Objects::nonNull)
                        .toList();
        fillUploaderNames(vos);
        return vos;
    }

    /** 批量回填上传人姓名（sys_user.name，空则 username）。 */
    private void fillUploaderNames(List<DocVersionVO> vos) {
        Set<Long> ids =
                vos.stream()
                        .map(DocVersionVO::getUploadedBy)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toCollection(LinkedHashSet::new));
        if (ids.isEmpty()) {
            return;
        }
        List<SysUser> users = userMapper.selectBatchIds(ids);
        if (users == null || users.isEmpty()) {
            return;
        }
        Map<Long, String> nameMap = new HashMap<>(users.size());
        for (SysUser user : users) {
            if (user == null || user.getId() == null) {
                continue;
            }
            nameMap.put(
                    user.getId(),
                    StringUtils.isNotBlank(user.getName()) ? user.getName() : user.getUsername());
        }
        for (DocVersionVO vo : vos) {
            Long uploadedBy = vo.getUploadedBy();
            if (uploadedBy != null) {
                vo.setUploadedByName(nameMap.get(uploadedBy));
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DocFileVO rollback(Long fileId, int versionNo) {
        DocFile file = requireFile(fileId);
        DocDirectory dir = requireDir(file.getDirId());
        requireEdit(dir, file);

        DocFileVersion target =
                versionMapper.selectOne(
                        new LambdaQueryWrapper<DocFileVersion>()
                                .eq(DocFileVersion::getFileId, fileId)
                                .eq(DocFileVersion::getVersionNo, versionNo)
                                .last("LIMIT 1"));
        if (target == null) {
            throw BusinessException.of(ErrorCode.VALID_ILLEGAL_ARGUMENT, "目标版本不存在：v" + versionNo);
        }

        byte[] content;
        try {
            content = java.nio.file.Files.readAllBytes(storageService.resolve(target.getStorageKey()));
        } catch (IOException e) {
            log.error("回滚读取版本文件失败：fileId={}，versionNo={}", fileId, versionNo, e);
            throw new BusinessException(ErrorCode.BIZ_UNPROCESSABLE, "目标版本文件读取失败", e);
        }
        String newKey = storageService.write(content, file.getExt());
        long newSize = target.getSize() == null ? content.length : target.getSize();
        String newChecksum = target.getChecksum();

        int previousVersionNo = file.getCurrentVersionNo() == null ? 0 : file.getCurrentVersionNo();
        int nextVersionNo = previousVersionNo + 1;
        Long operatorId = SecurityUtils.getCurrentUserIdOrDefault();
        String changeNote = "回滚到 v" + versionNo;
        int attempt = 0;
        while (true) {
            attempt++;
            DocFileVersion version = new DocFileVersion();
            version.setFileId(fileId);
            version.setVersionNo(nextVersionNo);
            version.setStorageKey(newKey);
            version.setSize(newSize);
            version.setChecksum(newChecksum);
            version.setUploadedBy(operatorId);
            version.setChangeNote(changeNote);
            version.setSource(DocVersionSource.ROLLBACK);
            try {
                versionMapper.insert(version);
                break;
            } catch (DuplicateKeyException e) {
                if (attempt >= MAX_VERSION_RETRY) {
                    log.warn("版本号冲突重试 {} 次仍失败：fileId={}，versionNo={}", attempt, fileId, nextVersionNo);
                    throw BusinessException.of(
                            ErrorCode.BIZ_STATE_CONFLICT, "版本号冲突，请稍后重试");
                }
                nextVersionNo++;
            }
        }

        UpdateWrapper<DocFile> wrapper =
                new UpdateWrapper<DocFile>()
                        .eq("id", fileId)
                        .set("size", newSize)
                        .set("checksum", newChecksum)
                        .set("current_version_no", nextVersionNo);
        fileMapper.update(null, wrapper);

        Map<String, Object> before = new LinkedHashMap<>();
        before.put("currentVersionNo", previousVersionNo);
        before.put("size", file.getSize());
        before.put("checksum", file.getChecksum());
        Map<String, Object> after = new LinkedHashMap<>();
        after.put("currentVersionNo", nextVersionNo);
        after.put("size", newSize);
        after.put("checksum", newChecksum);
        after.put("rollbackTo", versionNo);
        bpmAuditService.record(
                AUDIT_TARGET,
                fileId,
                AuditAction.DOC_ROLLBACK,
                before,
                after,
                null,
                operatorId);

        file.setSize(newSize);
        file.setChecksum(newChecksum);
        file.setCurrentVersionNo(nextVersionNo);
        // 回滚产生新版本：事务提交后重建语料，失败不影响回滚结果
        docIndexService.submitAfterCommit(fileId, nextVersionNo);
        log.info(
                "文档文件 {} 回滚到 v{}，新版本 v{}，key={}", fileId, versionNo, nextVersionNo, newKey);
        return DocConverter.toFileVO(file);
    }

    // ------------------------------------------------------------------
    // 内部工具
    // ------------------------------------------------------------------

    private DocFile requireFile(Long fileId) {
        DocFile file = fileId == null ? null : fileMapper.selectById(fileId);
        if (file == null) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "文件不存在");
        }
        return file;
    }

    private DocDirectory requireDir(Long dirId) {
        DocDirectory dir = dirId == null ? null : dirMapper.selectById(dirId);
        if (dir == null) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "目录不存在");
        }
        return dir;
    }

    /** 编辑权：命中 {@code doc:file:edit} 直接放行（管理员由守卫放行），否则按数据权限校验。 */
    private void requireEdit(DocDirectory dir, DocFile file) {
        if (securityGuard.hasPermission(PERMISSION_FILE_EDIT)) {
            return;
        }
        accessResolver.require(DocAction.EDIT, dir, file);
    }
}
