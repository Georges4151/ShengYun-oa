package com.ceams.module.doc.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.common.security.SecurityUtils;
import com.ceams.module.bpm.support.BpmSecurityGuard;
import com.ceams.module.doc.entity.DocDirPermission;
import com.ceams.module.doc.entity.DocDirectory;
import com.ceams.module.doc.entity.DocFile;
import com.ceams.module.doc.entity.DocFilePermission;
import com.ceams.module.doc.enums.DocAction;
import com.ceams.module.doc.enums.DocSubjectType;
import com.ceams.module.doc.enums.LibraryType;
import com.ceams.module.doc.mapper.DocDirPermissionMapper;
import com.ceams.module.doc.mapper.DocFilePermissionMapper;
import com.ceams.module.iam.entity.SysDept;
import com.ceams.module.iam.entity.SysUser;
import com.ceams.module.iam.entity.UserRole;
import com.ceams.module.iam.mapper.DeptMapper;
import com.ceams.module.iam.mapper.UserMapper;
import com.ceams.module.iam.mapper.UserRoleMapper;
import com.ceams.module.project.entity.Project;
import com.ceams.module.project.mapper.ProjectMapper;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 文档数据权限解析器：库默认规则 + 目录链授权 + 文件级授权取并集。
 *
 * <p>口径：
 *
 * <ol>
 *   <li>库默认规则：PERSONAL 仅 owner_user_id；DEPT 为 owner_dept_id 本部门及子部门（即该部门在用户部门祖先链上）；
 *       PROJECT 为 biz_project.manager_id；PUBLIC 全员。命中即给四类动作全集。
 *   <li>再沿目录链（由 doc_directory.path 解析出的祖先 ID 集合）合并 doc_dir_permission，
 *       并合并 doc_file_permission；不建模显式 deny。
 *   <li>{@code SYS_ADMIN} / {@code doc:permission:manage} 持有者全权。
 * </ol>
 *
 * <p>返回值为 EnumSet 副本，调用方可安全读取，不影响解析器内部状态。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DocAccessResolver {

    /** 全权权限码（仅 SYS_ADMIN 持有）。 */
    private static final String PERMISSION_FULL_ACCESS = "doc:permission:manage";

    /** 四类动作全集。 */
    private static final EnumSet<DocAction> ALL_ACTIONS = EnumSet.allOf(DocAction.class);

    private final DeptMapper deptMapper;
    private final DocDirPermissionMapper dirPermissionMapper;
    private final DocFilePermissionMapper filePermissionMapper;
    private final BpmSecurityGuard securityGuard;
    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final ProjectMapper projectMapper;

    /** 是否全权：拥有 {@code doc:permission:manage}（管理员直接放行）。 */
    public boolean isFullAccess() {
        return securityGuard.hasPermission(PERMISSION_FULL_ACCESS);
    }

    /** 当前登录用户所属部门 ID（未登录或未维护部门返回 null）。 */
    public Long currentUserDeptId() {
        Long userId = SecurityUtils.getCurrentUserId();
        return userId == null ? null : deptIdOf(userId);
    }

    /** 部门是否在用户可见范围内（本部门及其祖先链，管理员全权）。 */
    public boolean isDeptVisible(Long deptId) {
        if (deptId == null) {
            return false;
        }
        if (isFullAccess()) {
            return true;
        }
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return false;
        }
        return deptScope(deptIdOf(userId)).contains(deptId);
    }

    /** 当前用户是否为指定项目的项目经理（管理员全权）。 */
    public boolean isProjectManager(Long projectId) {
        if (isFullAccess()) {
            return true;
        }
        Long userId = SecurityUtils.getCurrentUserId();
        return userId != null && isProjectManager(userId, projectId);
    }

    /** 解析用户对目录 / 文件可执行的动作集合（dir / file 可为空）。 */
    public EnumSet<DocAction> resolve(Long userId, DocDirectory dir, DocFile file) {
        if (isFullAccess()) {
            return EnumSet.copyOf(ALL_ACTIONS);
        }
        if (userId == null) {
            return EnumSet.noneOf(DocAction.class);
        }
        SubjectContext context = loadContext(userId);
        EnumSet<DocAction> actions = EnumSet.noneOf(DocAction.class);
        if (dir != null && libraryGrants(dir, context)) {
            actions.addAll(ALL_ACTIONS);
        }
        if (dir != null) {
            actions.addAll(collect(toDirEntries(dirPermissionMapper.selectList(dirPermissionQuery(dir))), context));
        }
        if (file != null) {
            actions.addAll(
                    collect(
                            toFileEntries(
                                    filePermissionMapper.selectList(filePermissionQuery(file))),
                            context));
        }
        // EnumSet.copyOf 不接受空集合，空集时返回同类空集合
        return actions.isEmpty() ? EnumSet.noneOf(DocAction.class) : EnumSet.copyOf(actions);
    }

    /** 仅按目录解析动作集合。 */
    public EnumSet<DocAction> resolve(Long userId, DocDirectory dir) {
        return resolve(userId, dir, null);
    }

    /** 要求当前用户对目录 / 文件拥有指定动作，否则 403。 */
    public void require(DocAction action, DocDirectory dir, DocFile file) {
        if (action == null) {
            throw BusinessException.of(ErrorCode.VALID_ILLEGAL_ARGUMENT, "文档动作不能为空");
        }
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw BusinessException.of(ErrorCode.AUTH_ACCESS_DENIED, "未登录无权访问文档");
        }
        if (!resolve(userId, dir, file).contains(action)) {
            throw BusinessException.of(ErrorCode.AUTH_ACCESS_DENIED, "无权对该文档执行操作：" + action);
        }
    }

    /** 要求当前用户对目录拥有指定动作，否则 403。 */
    public void require(DocAction action, DocDirectory dir) {
        require(action, dir, null);
    }

    // ------------------------------------------------------------------
    // 内部实现
    // ------------------------------------------------------------------

    /** 库默认规则：命中返回 true（四类动作全集）。 */
    private boolean libraryGrants(DocDirectory dir, SubjectContext context) {
        LibraryType type = dir.getLibraryType();
        if (type == null) {
            return false;
        }
        return switch (type) {
            case PERSONAL -> context.userId().equals(dir.getOwnerUserId());
            case DEPT ->
                    dir.getOwnerDeptId() != null
                            && context.deptScope().contains(dir.getOwnerDeptId());
            case PROJECT ->
                    dir.getProjectId() != null
                            && isProjectManager(context.userId(), dir.getProjectId());
            case PUBLIC -> true;
        };
    }

    /** 沿目录链查询授权：path 形如 /1/2/3/，解析出 [1,2,3] 后一次 IN 查询。 */
    private LambdaQueryWrapper<DocDirPermission> dirPermissionQuery(DocDirectory dir) {
        Set<Long> dirIds = new LinkedHashSet<>();
        if (dir.getId() != null) {
            dirIds.add(dir.getId());
        }
        dirIds.addAll(parsePathIds(dir.getPath()));
        return new LambdaQueryWrapper<DocDirPermission>().in(DocDirPermission::getDirId, dirIds);
    }

    /** 文件级授权查询。 */
    private LambdaQueryWrapper<DocFilePermission> filePermissionQuery(DocFile file) {
        return new LambdaQueryWrapper<DocFilePermission>()
                .eq(DocFilePermission::getFileId, file.getId());
    }

    /** 解析路径中的目录 ID 链，非法段忽略并记 warn 日志。 */
    private Set<Long> parsePathIds(String path) {
        Set<Long> ids = new LinkedHashSet<>();
        if (path == null || path.isBlank()) {
            return ids;
        }
        for (String token : path.split("/")) {
            if (token.isBlank()) {
                continue;
            }
            try {
                ids.add(Long.valueOf(token.trim()));
            } catch (NumberFormatException e) {
                log.warn("目录路径存在非法片段，已忽略：{}", token);
            }
        }
        return ids;
    }

    /** 主体匹配：USER = 本人；ROLE = 用户角色；DEPT = 用户部门及其祖先链。 */
    private EnumSet<DocAction> collect(List<GrantEntry> entries, SubjectContext context) {
        EnumSet<DocAction> actions = EnumSet.noneOf(DocAction.class);
        for (GrantEntry entry : entries) {
            if (!matches(entry, context)) {
                continue;
            }
            actions.addAll(DocConverter.parseActions(entry.actions()));
        }
        return actions;
    }

    private boolean matches(GrantEntry entry, SubjectContext context) {
        DocSubjectType type = entry.subjectType();
        if (type == null || entry.subjectId() == null) {
            return false;
        }
        return switch (type) {
            case USER -> context.userId().equals(entry.subjectId());
            case ROLE -> context.roleIds().contains(entry.subjectId());
            case DEPT -> context.deptScope().contains(entry.subjectId());
        };
    }

    private List<GrantEntry> toDirEntries(List<DocDirPermission> permissions) {
        List<GrantEntry> entries = new ArrayList<>();
        if (permissions == null) {
            return entries;
        }
        for (DocDirPermission permission : permissions) {
            if (permission == null) {
                continue;
            }
            entries.add(
                    new GrantEntry(
                            permission.getSubjectType(),
                            permission.getSubjectId(),
                            permission.getActions()));
        }
        return entries;
    }

    private List<GrantEntry> toFileEntries(List<DocFilePermission> permissions) {
        List<GrantEntry> entries = new ArrayList<>();
        if (permissions == null) {
            return entries;
        }
        for (DocFilePermission permission : permissions) {
            if (permission == null) {
                continue;
            }
            entries.add(
                    new GrantEntry(
                            permission.getSubjectType(),
                            permission.getSubjectId(),
                            permission.getActions()));
        }
        return entries;
    }

    /** 装载主体上下文：用户 ID、部门 ID、可见部门集合、角色集合。 */
    private SubjectContext loadContext(Long userId) {
        Long deptId = deptIdOf(userId);
        Set<Long> roleIds = new LinkedHashSet<>();
        List<UserRole> links =
                userRoleMapper.selectList(
                        new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId));
        if (links != null) {
            for (UserRole link : links) {
                if (link != null && link.getRoleId() != null) {
                    roleIds.add(link.getRoleId());
                }
            }
        }
        return new SubjectContext(userId, deptId, deptScope(deptId), roleIds);
    }

    /** 用户所属部门 ID。 */
    private Long deptIdOf(Long userId) {
        SysUser user = userMapper.selectById(userId);
        return user == null ? null : user.getDeptId();
    }

    /** 部门自身及其祖先链（含环保护），无部门时返回空集。 */
    private Set<Long> deptScope(Long deptId) {
        Set<Long> scope = new LinkedHashSet<>();
        if (deptId == null) {
            return scope;
        }
        Map<Long, Long> parentById = loadDeptParents();
        Long cursor = deptId;
        while (cursor != null && scope.add(cursor)) {
            cursor = parentById.get(cursor);
        }
        return scope;
    }

    /** 一次查出全部部门并构建「部门 ID → 父部门 ID」映射，避免逐级 N 次查询。 */
    private Map<Long, Long> loadDeptParents() {
        List<SysDept> depts = deptMapper.selectList(new LambdaQueryWrapper<SysDept>());
        Map<Long, Long> parentById = new HashMap<>();
        if (depts == null) {
            return parentById;
        }
        for (SysDept dept : depts) {
            if (dept != null && dept.getId() != null) {
                parentById.put(dept.getId(), dept.getParentId());
            }
        }
        return parentById;
    }

    /** 项目经理判定：biz_project.manager_id 等于用户。 */
    private boolean isProjectManager(Long userId, Long projectId) {
        if (projectId == null) {
            return false;
        }
        Project project = projectMapper.selectById(projectId);
        return project != null && Objects.equals(userId, project.getManagerId());
    }

    /** 授权条目（目录 / 文件级共用）。 */
    private record GrantEntry(DocSubjectType subjectType, Long subjectId, String actions) {}

    /** 主体上下文。 */
    private record SubjectContext(
            Long userId, Long deptId, Set<Long> deptScope, Set<Long> roleIds) {}
}
