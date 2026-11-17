package com.ceams.module.doc.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.common.security.SecurityUtils;
import com.ceams.module.bpm.support.BpmSecurityGuard;
import com.ceams.module.doc.dto.DirectoryCreateRequest;
import com.ceams.module.doc.dto.DirectoryMoveRequest;
import com.ceams.module.doc.dto.DirectoryUpdateRequest;
import com.ceams.module.doc.entity.DocDirectory;
import com.ceams.module.doc.entity.DocFile;
import com.ceams.module.doc.enums.DocAction;
import com.ceams.module.doc.enums.LibraryType;
import com.ceams.module.doc.mapper.DocDirectoryMapper;
import com.ceams.module.doc.mapper.DocFileMapper;
import com.ceams.module.doc.service.DocDirectoryService;
import com.ceams.module.doc.support.DocAccessResolver;
import com.ceams.module.doc.support.DocConverter;
import com.ceams.module.doc.support.DocPathUtil;
import com.ceams.module.doc.vo.DirectoryNodeVO;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 文档目录服务实现。
 *
 * <p>关键规则：同级重名拒绝、跨库移动拒绝、禁止移到自身或子树、非空目录禁止删除；
 * 目录树的可见性由 {@link DocAccessResolver} 逐节点过滤（无 PREVIEW 即隐藏）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocDirectoryServiceImpl implements DocDirectoryService {

    /** 目录管理权限码（V39 种子，仅 SYS_ADMIN）。 */
    private static final String PERMISSION_DIR_MANAGE = "doc:dir:manage";

    /** 目录状态：正常。 */
    private static final String STATUS_NORMAL = "NORMAL";

    private final DocDirectoryMapper dirMapper;
    private final DocFileMapper fileMapper;
    private final DocAccessResolver accessResolver;
    private final BpmSecurityGuard securityGuard;

    @Override
    public List<DirectoryNodeVO> tree(String libraryType, Long ownerId) {
        LibraryType type = LibraryType.from(libraryType);
        if (type == null) {
            throw BusinessException.of(ErrorCode.VALID_ILLEGAL_ARGUMENT, "库类型非法：" + libraryType);
        }
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw BusinessException.of(ErrorCode.AUTH_ACCESS_DENIED, "未登录不可查看文档目录");
        }

        LambdaQueryWrapper<DocDirectory> wrapper =
                new LambdaQueryWrapper<DocDirectory>()
                        .eq(DocDirectory::getLibraryType, type)
                        .orderByAsc(DocDirectory::getSortOrder)
                        .orderByAsc(DocDirectory::getId);
        switch (type) {
            case PERSONAL -> wrapper.eq(
                    DocDirectory::getOwnerUserId, ownerId == null ? userId : ownerId);
            case DEPT -> {
                Long deptId = ownerId == null ? accessResolver.currentUserDeptId() : ownerId;
                if (deptId == null) {
                    return List.of();
                }
                wrapper.eq(DocDirectory::getOwnerDeptId, deptId);
            }
            case PROJECT -> {
                if (ownerId == null) {
                    return List.of();
                }
                wrapper.eq(DocDirectory::getProjectId, ownerId);
            }
            case PUBLIC -> {
                // 公共库忽略 ownerId
            }
        }

        List<DocDirectory> dirs = dirMapper.selectList(wrapper);
        if (dirs == null || dirs.isEmpty()) {
            return List.of();
        }
        List<DocDirectory> visible =
                dirs.stream()
                        .filter(dir -> accessResolver.resolve(userId, dir, null).contains(DocAction.PREVIEW))
                        .toList();

        Map<Long, DirectoryNodeVO> voMap = new LinkedHashMap<>();
        for (DocDirectory dir : visible) {
            voMap.put(dir.getId(), DocConverter.toNode(dir));
        }
        List<DirectoryNodeVO> roots = new ArrayList<>();
        for (DocDirectory dir : visible) {
            DirectoryNodeVO vo = voMap.get(dir.getId());
            DirectoryNodeVO parent = dir.getParentId() == null ? null : voMap.get(dir.getParentId());
            if (parent != null && parent != vo) {
                parent.getChildren().add(vo);
            } else {
                // 根目录，或父目录不可见（降级为根，避免节点丢失）
                roots.add(vo);
            }
        }
        roots.forEach(this::sortChildren);
        return roots;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String create(DirectoryCreateRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw BusinessException.of(ErrorCode.AUTH_ACCESS_DENIED, "未登录不可创建目录");
        }
        String name = StringUtils.trimToNull(request.getName());
        if (name == null) {
            throw BusinessException.of(ErrorCode.VALID_REQUIRED_FIELD, "目录名称不能为空");
        }
        LibraryType type = request.getLibraryType();
        if (type == null) {
            throw BusinessException.of(ErrorCode.VALID_REQUIRED_FIELD, "库类型不能为空");
        }

        Long ownerUserId = null;
        Long ownerDeptId = null;
        Long projectId = null;
        switch (type) {
            case PERSONAL -> {
                if (request.getOwnerUserId() != null && !request.getOwnerUserId().equals(userId)) {
                    throw BusinessException.of(ErrorCode.AUTH_ACCESS_DENIED, "个人库仅允许本人创建");
                }
                ownerUserId = userId;
            }
            case DEPT -> {
                Long deptId =
                        request.getOwnerDeptId() == null
                                ? accessResolver.currentUserDeptId()
                                : request.getOwnerDeptId();
                if (deptId == null) {
                    throw BusinessException.of(
                            ErrorCode.VALID_REQUIRED_FIELD, "部门库必须指定归属部门");
                }
                if (!accessResolver.isDeptVisible(deptId)) {
                    throw BusinessException.of(ErrorCode.AUTH_ACCESS_DENIED, "无权在该部门下创建目录");
                }
                ownerDeptId = deptId;
            }
            case PROJECT -> {
                if (request.getProjectId() == null) {
                    throw BusinessException.of(ErrorCode.VALID_REQUIRED_FIELD, "项目库必须指定项目");
                }
                if (!accessResolver.isProjectManager(request.getProjectId())) {
                    throw BusinessException.of(ErrorCode.AUTH_ACCESS_DENIED, "仅项目经理可创建项目库目录");
                }
                projectId = request.getProjectId();
            }
            case PUBLIC -> {
                if (!accessResolver.isFullAccess()) {
                    throw BusinessException.of(ErrorCode.AUTH_ACCESS_DENIED, "仅管理员可创建公共库目录");
                }
            }
        }

        Long parentId = request.getParentId();
        String parentPath = "";
        if (parentId != null) {
            DocDirectory parent = requireDir(parentId);
            boolean sameLibrary =
                    parent.getLibraryType() == type
                            && Objects.equals(parent.getOwnerUserId(), ownerUserId)
                            && Objects.equals(parent.getOwnerDeptId(), ownerDeptId)
                            && Objects.equals(parent.getProjectId(), projectId);
            if (!sameLibrary) {
                throw BusinessException.of(
                        ErrorCode.VALID_ILLEGAL_ARGUMENT, "父目录不存在或不属于目标库");
            }
            parentPath = parent.getPath();
        }
        ensureNameUnique(name, parentId, type, ownerUserId, ownerDeptId, projectId, null);

        // 路径依赖主键，先取雪花 ID 再落库（path 列 NOT NULL）
        Long id = IdWorker.getId();
        DocDirectory dir = new DocDirectory();
        dir.setId(id);
        dir.setName(name);
        dir.setParentId(parentId);
        dir.setLibraryType(type);
        dir.setOwnerUserId(ownerUserId);
        dir.setOwnerDeptId(ownerDeptId);
        dir.setProjectId(projectId);
        dir.setPath(DocPathUtil.childPath(parentPath, id));
        dir.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
        dir.setStatus(STATUS_NORMAL);
        dirMapper.insert(dir);
        log.info("新建文档目录：id={}，name={}，libraryType={}", id, name, type);
        return String.valueOf(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, DirectoryUpdateRequest request) {
        DocDirectory dir = requireDir(id);
        requireManageOrAction(dir, DocAction.EDIT);

        String name = dir.getName();
        if (request.getName() != null) {
            String newName = StringUtils.trimToNull(request.getName());
            if (newName == null) {
                throw BusinessException.of(ErrorCode.VALID_REQUIRED_FIELD, "目录名称不能为空");
            }
            if (!newName.equals(dir.getName())) {
                ensureNameUnique(
                        newName,
                        dir.getParentId(),
                        dir.getLibraryType(),
                        dir.getOwnerUserId(),
                        dir.getOwnerDeptId(),
                        dir.getProjectId(),
                        id);
            }
            name = newName;
        }
        dir.setName(name);
        if (request.getSortOrder() != null) {
            dir.setSortOrder(request.getSortOrder());
        }
        dirMapper.updateById(dir);
        log.info("文档目录 {} 已更新：name={}，sortOrder={}", id, name, request.getSortOrder());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void move(Long id, DirectoryMoveRequest request) {
        DocDirectory dir = requireDir(id);
        requireManageOrAction(dir, DocAction.EDIT);

        Long targetParentId = request.getParentId();
        if (targetParentId != null && targetParentId.equals(id)) {
            throw BusinessException.of(ErrorCode.VALID_ILLEGAL_ARGUMENT, "不能移动到自身或子目录");
        }
        String newParentPath = "";
        if (targetParentId != null) {
            DocDirectory parent = requireDir(targetParentId);
            if (DocPathUtil.isSelfOrDescendant(id, parent.getPath())) {
                throw BusinessException.of(ErrorCode.VALID_ILLEGAL_ARGUMENT, "不能移动到自身或子目录");
            }
            if (!sameLibrary(dir, parent)) {
                throw BusinessException.of(ErrorCode.VALID_ILLEGAL_ARGUMENT, "不允许跨库移动目录");
            }
            newParentPath = parent.getPath();
            if (!Objects.equals(targetParentId, dir.getParentId())) {
                ensureNameUnique(
                        dir.getName(),
                        targetParentId,
                        dir.getLibraryType(),
                        dir.getOwnerUserId(),
                        dir.getOwnerDeptId(),
                        dir.getProjectId(),
                        id);
            }
        }

        String oldSelfPath = dir.getPath();
        String newSelfPath = DocPathUtil.childPath(newParentPath, id);
        if (Objects.equals(oldSelfPath, newSelfPath)
                && Objects.equals(dir.getParentId(), targetParentId)) {
            log.info("文档目录 {} 移动后路径未变化，跳过刷新", id);
            return;
        }

        // 子树路径批量刷新：path LIKE '/id/%' 查出全部后代（含自身）后逐条改前缀
        List<DocDirectory> subtree =
                dirMapper.selectList(
                        new LambdaQueryWrapper<DocDirectory>()
                                .likeRight(DocDirectory::getPath, oldSelfPath));
        int updated = 0;
        if (subtree != null) {
            for (DocDirectory node : subtree) {
                String rebased = DocPathUtil.rebase(node.getPath(), oldSelfPath, newSelfPath);
                if (Objects.equals(rebased, node.getPath()) && !node.getId().equals(id)) {
                    continue;
                }
                DocDirectory patch = new DocDirectory();
                patch.setId(node.getId());
                patch.setPath(rebased);
                // 乐观锁版本置空，避免条件更新失效
                patch.setVersion(null);
                if (node.getId().equals(id)) {
                    patch.setParentId(targetParentId);
                }
                dirMapper.updateById(patch);
                updated++;
            }
        }
        log.info(
                "文档目录 {} 已移动：parentId={}，path={}，刷新子树 {} 条",
                id,
                targetParentId,
                newSelfPath,
                updated);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        DocDirectory dir = requireDir(id);
        requireManageOrAction(dir, DocAction.DELETE);

        Long children =
                dirMapper.selectCount(
                        new LambdaQueryWrapper<DocDirectory>()
                                .eq(DocDirectory::getParentId, id));
        if (children != null && children > 0) {
            throw BusinessException.of(
                    ErrorCode.VALID_ILLEGAL_ARGUMENT, "目录下存在子目录，请先清空后再删除");
        }
        Long files =
                fileMapper.selectCount(
                        new LambdaQueryWrapper<DocFile>().eq(DocFile::getDirId, id));
        if (files != null && files > 0) {
            throw BusinessException.of(
                    ErrorCode.VALID_ILLEGAL_ARGUMENT, "目录下存在文件，请先清空后再删除");
        }
        dirMapper.deleteById(id);
        log.info("文档目录 {} 已删除：name={}", id, dir.getName());
    }

    // ------------------------------------------------------------------
    // 内部工具
    // ------------------------------------------------------------------

    private DocDirectory requireDir(Long id) {
        DocDirectory dir = id == null ? null : dirMapper.selectById(id);
        if (dir == null) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "目录不存在");
        }
        return dir;
    }

    /** 目录写操作权限：doc:dir:manage 或有目录级对应动作。 */
    private void requireManageOrAction(DocDirectory dir, DocAction action) {
        if (securityGuard.hasPermission(PERMISSION_DIR_MANAGE)) {
            return;
        }
        accessResolver.require(action, dir, null);
    }

    /** 同库判定：库类型与三个 owner 字段必须完全一致。 */
    private boolean sameLibrary(DocDirectory left, DocDirectory right) {
        return left.getLibraryType() == right.getLibraryType()
                && Objects.equals(left.getOwnerUserId(), right.getOwnerUserId())
                && Objects.equals(left.getOwnerDeptId(), right.getOwnerDeptId())
                && Objects.equals(left.getProjectId(), right.getProjectId());
    }

    /** 同级目录名称唯一（deleted=0 范围内，逻辑删除由 MyBatis-Plus 自动过滤）。 */
    private void ensureNameUnique(
            String name,
            Long parentId,
            LibraryType type,
            Long ownerUserId,
            Long ownerDeptId,
            Long projectId,
            Long excludeId) {
        LambdaQueryWrapper<DocDirectory> wrapper =
                new LambdaQueryWrapper<DocDirectory>()
                        .eq(DocDirectory::getLibraryType, type)
                        .eq(DocDirectory::getName, name);
        if (parentId == null) {
            wrapper.isNull(DocDirectory::getParentId);
        } else {
            wrapper.eq(DocDirectory::getParentId, parentId);
        }
        if (ownerUserId == null) {
            wrapper.isNull(DocDirectory::getOwnerUserId);
        } else {
            wrapper.eq(DocDirectory::getOwnerUserId, ownerUserId);
        }
        if (ownerDeptId == null) {
            wrapper.isNull(DocDirectory::getOwnerDeptId);
        } else {
            wrapper.eq(DocDirectory::getOwnerDeptId, ownerDeptId);
        }
        if (projectId == null) {
            wrapper.isNull(DocDirectory::getProjectId);
        } else {
            wrapper.eq(DocDirectory::getProjectId, projectId);
        }
        if (excludeId != null) {
            wrapper.ne(DocDirectory::getId, excludeId);
        }
        Long count = dirMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw BusinessException.of(
                    ErrorCode.VALID_ILLEGAL_ARGUMENT, "同级下已存在同名目录：" + name);
        }
    }

    /** 子节点排序：sortOrder 升序（空值靠后），同序号按 ID 升序。 */
    private void sortChildren(DirectoryNodeVO node) {
        node.getChildren()
                .sort(
                        Comparator.comparing(
                                        DirectoryNodeVO::getSortOrder,
                                        Comparator.nullsLast(Comparator.naturalOrder()))
                                .thenComparing(
                                        DirectoryNodeVO::getId,
                                        Comparator.nullsLast(Comparator.naturalOrder())));
        for (DirectoryNodeVO child : node.getChildren()) {
            sortChildren(child);
        }
    }
}
