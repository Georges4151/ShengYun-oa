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
import com.ceams.module.doc.mapper.DocDirectoryMapper;
import com.ceams.module.doc.mapper.DocFileMapper;
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
import java.util.Collections;
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
    private final DocDirectoryMapper dirMapper;
    private final DocFileMapper fileMapper;
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

    /**
     * 批量解析用户可见的文件 ID 集合（知识库检索的「先收窄再召回」入口）。
     *
     * <p>口径与 {@link #resolve(Long, DocDirectory, DocFile)} 完全一致，只是把「逐文件判定」
     * 改为「先按目录批量判定，再反查文件」：
     *
     * <ol>
     *   <li>全权用户：直接取过滤条件下所有未删除目录下的未删除文件；
     *   <li>库默认规则（PERSONAL 本人 / DEPT 部门在可见链 / PROJECT 项目经理 / PUBLIC 全员）
     *       或目录链（含自身）上主体匹配授权包含 {@code PREVIEW} 的目录，其下文件全部可见；
     *   <li>文件级授权中主体匹配且包含 {@code PREVIEW} 的文件也并入；若指定了
     *       {@code libraryType / projectId}，只保留其目录符合过滤条件的文件。
     * </ol>
     *
     * <p>返回值不可变；无权限时返回空集合（调用方必须直接返回空结果，不得先召回后过滤）。
     *
     * @param userId 用户 ID（为空返回空集合）
     * @param libraryType 库类型过滤（可空表示不过滤）
     * @param projectId 项目 ID 过滤（可空表示不过滤）
     * @return 不可变的可访问文件 ID 集合
     */
    public Set<Long> accessibleFileIds(Long userId, LibraryType libraryType, Long projectId) {
        if (userId == null) {
            return Set.of();
        }
        List<DocDirectory> dirs = loadDirectories(libraryType, projectId);
        if (dirs.isEmpty()) {
            return Set.of();
        }
        Set<Long> filteredDirIds = new LinkedHashSet<>();
        for (DocDirectory dir : dirs) {
            if (dir.getId() != null) {
                filteredDirIds.add(dir.getId());
            }
        }
        if (filteredDirIds.isEmpty()) {
            return Set.of();
        }

        if (isFullAccess()) {
            return Collections.unmodifiableSet(
                    new LinkedHashSet<>(fileIdsUnderDirs(filteredDirIds)));
        }

        SubjectContext context = loadContext(userId);
        Map<Long, List<GrantEntry>> grantsByDir = loadAllDirGrants();
        Map<Long, Boolean> projectManagerCache = new HashMap<>();
        Set<Long> allowedDirIds = new LinkedHashSet<>();
        for (DocDirectory dir : dirs) {
            if (dir == null || dir.getId() == null) {
                continue;
            }
            if (libraryGrants(dir, context, projectManagerCache)
                    || grantsPreviewOnChain(dir, grantsByDir, context)) {
                allowedDirIds.add(dir.getId());
            }
        }

        Set<Long> fileIds = new LinkedHashSet<>(fileIdsUnderDirs(allowedDirIds));
        fileIds.addAll(fileIdsGrantedPreview(filteredDirIds, context));
        return Collections.unmodifiableSet(fileIds);
    }

    // ------------------------------------------------------------------
    // 内部实现
    // ------------------------------------------------------------------

    /** 按库类型 / 项目过滤加载全部未删除目录。 */
    private List<DocDirectory> loadDirectories(LibraryType libraryType, Long projectId) {
        LambdaQueryWrapper<DocDirectory> wrapper = new LambdaQueryWrapper<>();
        if (libraryType != null) {
            wrapper.eq(DocDirectory::getLibraryType, libraryType);
        }
        if (projectId != null) {
            wrapper.eq(DocDirectory::getProjectId, projectId);
        }
        List<DocDirectory> dirs = dirMapper.selectList(wrapper);
        return dirs == null ? List.of() : dirs;
    }

    /** 指定目录下所有未删除文件 ID（目录集合为空时直接返回空，避免 IN ()）。 */
    private Set<Long> fileIdsUnderDirs(Set<Long> dirIds) {
        Set<Long> fileIds = new LinkedHashSet<>();
        if (dirIds == null || dirIds.isEmpty()) {
            return fileIds;
        }
        List<DocFile> files =
                fileMapper.selectList(
                        new LambdaQueryWrapper<DocFile>()
                                .select(DocFile::getId, DocFile::getDirId)
                                .in(DocFile::getDirId, dirIds));
        if (files == null) {
            return fileIds;
        }
        for (DocFile file : files) {
            if (file != null && file.getId() != null) {
                fileIds.add(file.getId());
            }
        }
        return fileIds;
    }

    /**
     * 文件级授权命中 PREVIEW 的文件 ID。
     *
     * <p>过滤条件存在时，只保留目录属于 {@code filteredDirIds} 的文件。
     */
    private Set<Long> fileIdsGrantedPreview(Set<Long> filteredDirIds, SubjectContext context) {
        Set<Long> grantedFileIds = new LinkedHashSet<>();
        List<DocFilePermission> permissions = loadOwnFilePermissions(context);
        if (permissions == null) {
            return grantedFileIds;
        }
        Set<Long> candidateFileIds = new LinkedHashSet<>();
        for (DocFilePermission permission : permissions) {
            if (permission == null || permission.getFileId() == null) {
                continue;
            }
            GrantEntry entry =
                    new GrantEntry(
                            permission.getSubjectType(),
                            permission.getSubjectId(),
                            permission.getActions());
            if (!matches(entry, context)
                    || !DocConverter.parseActions(entry.actions()).contains(DocAction.PREVIEW)) {
                continue;
            }
            candidateFileIds.add(permission.getFileId());
        }
        if (candidateFileIds.isEmpty()) {
            return grantedFileIds;
        }
        List<DocFile> files = fileMapper.selectBatchIds(candidateFileIds);
        if (files == null) {
            return grantedFileIds;
        }
        for (DocFile file : files) {
            if (file == null || file.getId() == null) {
                continue;
            }
            if (file.getDirId() == null || !filteredDirIds.contains(file.getDirId())) {
                continue;
            }
            grantedFileIds.add(file.getId());
        }
        return grantedFileIds;
    }

    /** 一次性加载全量目录授权，按目录 ID 分组（避免逐个目录查询）。 */
    private Map<Long, List<GrantEntry>> loadAllDirGrants() {
        Map<Long, List<GrantEntry>> grantsByDir = new HashMap<>();
        List<DocDirPermission> permissions =
                dirPermissionMapper.selectList(new LambdaQueryWrapper<>());
        if (permissions == null) {
            return grantsByDir;
        }
        for (DocDirPermission permission : permissions) {
            if (permission == null || permission.getDirId() == null) {
                continue;
            }
            grantsByDir
                    .computeIfAbsent(permission.getDirId(), key -> new ArrayList<>())
                    .add(
                            new GrantEntry(
                                    permission.getSubjectType(),
                                    permission.getSubjectId(),
                                    permission.getActions()));
        }
        return grantsByDir;
    }

    /** 目录自身加上祖先链（path 解析）上是否存在主体匹配且含 PREVIEW 的授权。 */
    private boolean grantsPreviewOnChain(
            DocDirectory dir, Map<Long, List<GrantEntry>> grantsByDir, SubjectContext context) {
        Set<Long> chain = new LinkedHashSet<>();
        if (dir.getId() != null) {
            chain.add(dir.getId());
        }
        chain.addAll(parsePathIds(dir.getPath()));
        for (Long dirId : chain) {
            List<GrantEntry> entries = grantsByDir.get(dirId);
            if (entries == null || entries.isEmpty()) {
                continue;
            }
            if (collect(entries, context).contains(DocAction.PREVIEW)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 一次性加载「主体可能匹配」的文件级授权（SQL 侧先按 USER / ROLE / DEPT 收窄）。
     *
     * <p>SQL 只是缩小范围，是否真正命中主体仍由 {@link #matches} 复核。
     */
    private List<DocFilePermission> loadOwnFilePermissions(SubjectContext context) {
        Set<Long> roleIds = context.roleIds();
        Set<Long> deptScope = context.deptScope();
        LambdaQueryWrapper<DocFilePermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(
                w -> {
                    w.and(
                            user ->
                                    user.eq(
                                                    DocFilePermission::getSubjectType,
                                                    DocSubjectType.USER)
                                            .eq(DocFilePermission::getSubjectId, context.userId()));
                    if (roleIds != null && !roleIds.isEmpty()) {
                        w.or(
                                role ->
                                        role.eq(
                                                        DocFilePermission::getSubjectType,
                                                        DocSubjectType.ROLE)
                                                .in(DocFilePermission::getSubjectId, roleIds));
                    }
                    if (deptScope != null && !deptScope.isEmpty()) {
                        w.or(
                                dept ->
                                        dept.eq(
                                                        DocFilePermission::getSubjectType,
                                                        DocSubjectType.DEPT)
                                                .in(DocFilePermission::getSubjectId, deptScope));
                    }
                });
        return filePermissionMapper.selectList(wrapper);
    }

    /** 库默认规则：命中返回 true（四类动作全集）。 */
    private boolean libraryGrants(DocDirectory dir, SubjectContext context) {
        return libraryGrants(dir, context, new HashMap<>());
    }

    /**
     * 库默认规则（带项目经理判定缓存）。
     *
     * <p>批量判定时同一项目会重复命中，缓存可把 N 次 {@code biz_project} 查询降为 1 次。
     */
    private boolean libraryGrants(
            DocDirectory dir, SubjectContext context, Map<Long, Boolean> projectManagerCache) {
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
                            && projectManagerCache.computeIfAbsent(
                                    dir.getProjectId(),
                                    projectId -> isProjectManager(context.userId(), projectId));
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
