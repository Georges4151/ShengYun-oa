package com.ceams.module.bpm.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.common.security.SecurityUtils;
import com.ceams.module.bpm.enums.BpmBizType;
import com.ceams.module.bpm.enums.BpmDataScope;
import com.ceams.module.iam.entity.SysRole;
import com.ceams.module.iam.entity.SysUser;
import com.ceams.module.iam.entity.UserRole;
import com.ceams.module.iam.enums.RoleDataScope;
import com.ceams.module.iam.mapper.RoleMapper;
import com.ceams.module.iam.mapper.UserMapper;
import com.ceams.module.iam.mapper.UserRoleMapper;
import com.ceams.module.iam.service.PermissionService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * BPM 权限守卫：服务层归属校验、权限码校验与数据范围判定，与控制器 @PreAuthorize 双重防护。
 *
 * <p>现有 JWT 仅携带 ROLE_* 角色，不带权限码，因此权限码统一在服务层通过 IAM
 * PermissionService 实时查询；管理员判定仍基于 ROLE_SYS_ADMIN，便于单元测试与快速放行。
 */
@Component
@RequiredArgsConstructor
public class BpmSecurityGuard {

    private final PermissionService permissionService;
    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;
    private final BpmOrgResolver orgResolver;

    /** 当前登录用户 ID（未登录返回 null）。 */
    public Long currentUserId() {
        return SecurityUtils.getCurrentUserId();
    }

    /** 当前登录用户 ID，未登录时按系统处理（仅用于审计字段兜底，不用于归属判断）。 */
    public Long currentUserIdOrDefault() {
        return SecurityUtils.getCurrentUserIdOrDefault();
    }

    /** 是否系统管理员（ROLE_SYS_ADMIN）。 */
    public boolean isAdmin() {
        return currentRoles().contains("ROLE_SYS_ADMIN");
    }

    /** 当前用户角色集合（形如 ROLE_SYS_ADMIN）。 */
    public List<String> currentRoles() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Collections.emptyList();
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
    }

    /** 当前用户是否拥有指定权限码（管理员直接放行）。 */
    public boolean hasPermission(String permissionCode) {
        if (isAdmin()) {
            return true;
        }
        if (permissionCode == null || permissionCode.isBlank()) {
            return false;
        }
        return permissionService.currentUserPermissionCodes().contains(permissionCode);
    }

    /** 要求管理员权限，否则 403。 */
    public void requireAdmin() {
        if (!isAdmin()) {
            throw BusinessException.of(ErrorCode.AUTH_ACCESS_DENIED, "仅管理员可执行该操作");
        }
    }

    /** 要求管理员或指定权限码，否则 403。 */
    public void requireAdminOrPermission(String permissionCode) {
        if (!hasPermission(permissionCode)) {
            throw BusinessException.of(ErrorCode.AUTH_ACCESS_DENIED, "缺少权限：" + permissionCode);
        }
    }

    /** 要求指定权限码（管理员放行），否则 403。 */
    public void requirePermission(String permissionCode) {
        requireAdminOrPermission(permissionCode);
    }

    /**
     * 校验发起人对业务类型是否有权：管理员放行；报销要求 expense:submit，
     * 项目变更要求 project:create/project:update；HR 类申请由业务接口自行控制范围
     * （入转调离为管理员接口，请假/补卡允许员工为本人发起），此处一律放行；
     * 未知业务类型仅管理员可发起。
     */
    public void requireBizPermission(String bizType) {
        if (isAdmin()) {
            return;
        }
        BpmBizType type = BpmBizType.from(bizType);
        if (type == null) {
            throw BusinessException.of(ErrorCode.AUTH_ACCESS_DENIED, "未知业务类型，无权发起");
        }
        boolean allowed = switch (type) {
            case EXPENSE -> hasPermission("expense:submit");
            case CHANGE_REQUEST -> hasPermission("project:create") || hasPermission("project:update");
            case HR_REGULAR,
                    HR_TRANSFER,
                    HR_RESIGN,
                    HR_LEAVE,
                    HR_CHECKIN_SUPPLEMENT,
                    HR_SALARY_CHANGE,
                    HR_SALARY_CORRECTION,
                    HR_PERFORMANCE,
                    ADMIN_ASSET_BORROW,
                    ADMIN_MATERIAL_APPLY,
                    ADMIN_VEHICLE_APPLY,
                    ADMIN_MEETING_APPLY,
                    ADMIN_REPAIR -> true;
        };
        if (!allowed) {
            throw BusinessException.of(ErrorCode.AUTH_ACCESS_DENIED, "无权发起该业务类型流程：" + type);
        }
    }

    /**
     * 当前用户数据范围：读取其全部角色的 data_scope，取最宽（ALL &gt; DEPT &gt; SELF）。
     *
     * <p>无角色、空值或非法值一律回退 SELF；普通员工即使维护了 dept_id，
     * 只要角色范围为 SELF 也只能看到本人数据。部门范围口径为“同部门及子部门”。
     */
    public BpmDataScope resolveDataScope() {
        return switch (widestRoleDataScope()) {
            case ALL -> BpmDataScope.ALL;
            case DEPT -> BpmDataScope.DEPT;
            default -> BpmDataScope.SELF;
        };
    }

    /**
     * 当前用户可见的用户 ID 集合。
     *
     * <p>返回 {@code null} 表示不限制（ALL）；DEPT 返回本部门及子部门全部用户；
     * SELF 返回仅本人。调用方需处理 null 与空集合。
     */
    public List<Long> visibleUserIds() {
        BpmDataScope scope = resolveDataScope();
        if (scope == BpmDataScope.ALL) {
            return null;
        }
        Long current = currentUserId();
        if (current == null) {
            return List.of();
        }
        if (scope != BpmDataScope.DEPT) {
            return List.of(current);
        }
        SysUser user = currentUser();
        if (user == null || user.getDeptId() == null) {
            // 角色给了部门范围但未维护部门：降级为仅本人，避免越权
            return List.of(current);
        }
        List<Long> userIds = new ArrayList<>(orgResolver.deptUserIds(user.getDeptId()));
        if (!userIds.contains(current)) {
            // 防御：部门数据异常或当前用户不在查询结果时，至少保证本人可见
            userIds.add(current);
        }
        return userIds;
    }

    /** 当前用户所有角色中最宽的数据范围；解析失败或非法值忽略，无有效值回退 SELF。 */
    private RoleDataScope widestRoleDataScope() {
        Long current = currentUserId();
        if (current == null) {
            return RoleDataScope.SELF;
        }
        List<UserRole> links =
                userRoleMapper.selectList(
                        new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, current));
        if (links == null || links.isEmpty()) {
            return RoleDataScope.SELF;
        }
        List<Long> roleIds =
                links.stream().map(UserRole::getRoleId).filter(Objects::nonNull).distinct().toList();
        if (roleIds.isEmpty()) {
            return RoleDataScope.SELF;
        }
        List<SysRole> roles = roleMapper.selectBatchIds(roleIds);
        RoleDataScope widest = RoleDataScope.SELF;
        if (roles == null) {
            return widest;
        }
        for (SysRole role : roles) {
            if (role == null) {
                continue;
            }
            RoleDataScope scope = role.getDataScope();
            if (scope == null) {
                // 空值/历史非法值：按 SELF 处理，不影响其他角色取最宽
                continue;
            }
            if (scope.weight() > widest.weight()) {
                widest = scope;
            }
        }
        return widest;
    }

    private SysUser currentUser() {
        Long current = currentUserId();
        return current == null ? null : userMapper.selectById(current);
    }

    /** 要求任务归属当前用户，否则 403（越权查看/处理他人待办）。 */
    public void requireTaskOwner(Long assigneeId) {
        Long current = currentUserId();
        if (current == null || assigneeId == null || !current.equals(assigneeId)) {
            throw BusinessException.of(ErrorCode.BPM_TASK_NOT_OWNED, "非本人待办，无权操作");
        }
    }
}
