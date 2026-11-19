package com.ceams.module.ai.tool;

import com.ceams.common.security.SecurityUtils;
import java.util.List;
import java.util.Locale;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 只读工具的安全上下文辅助方法。
 *
 * <p>角色来源与 {@code JwtAuthenticationFilter} 保持一致：authorities 为 {@code ROLE_<角色>}。
 * 未认证时一律按受限角色处理，避免误放开财务数据。
 */
public final class ToolSecurity {

    private static final String ROLE_PREFIX = "ROLE_";

    private static final List<String> FINANCE_ROLES = List.of("FINANCE", "SYS_ADMIN");

    private ToolSecurity() {}

    /** 当前登录用户 ID（未认证时返回 null）。 */
    public static Long currentUserId() {
        return SecurityUtils.getCurrentUserId();
    }

    /** 是否为系统管理员（可查看全量人力、行政等管理数据）。 */
    public static boolean isAdmin() {
        return hasAnyRole("SYS_ADMIN");
    }

    /** 是否为财务角色（不含系统管理员，便于区分口径）。 */
    public static boolean isFinance() {
        return hasAnyRole("FINANCE");
    }

    /** 是否为财务或系统管理员（可查看全量资金、银行、印章数据）。 */
    public static boolean isFinanceOrAdmin() {
        return hasAnyRole(FINANCE_ROLES);
    }

    /** 判断当前登录用户是否拥有任一角色（忽略大小写）。 */
    public static boolean hasAnyRole(String... roles) {
        return hasAnyRole(List.of(roles));
    }

    private static boolean hasAnyRole(List<String> roles) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        List<String> expected =
                roles.stream().map(role -> ROLE_PREFIX + role.toUpperCase(Locale.ROOT)).toList();
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            if (expected.contains(authority.getAuthority().toUpperCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }
}
