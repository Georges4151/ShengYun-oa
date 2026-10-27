package com.ceams.common.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 安全上下文工具：获取当前登录用户信息。
 *
 * <p>当前骨架阶段 principal 由 {@link JwtAuthenticationFilter} 设置为用户 ID（Long）。
 * 未认证时返回 {@code null}，调用方自行兜底。</p>
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    /** 当前登录用户 ID（未认证时返回 null）。 */
    public static Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof Long longVal) {
            return longVal;
        }
        if (principal instanceof String str) {
            try {
                return Long.parseLong(str);
            } catch (NumberFormatException ignore) {
                return null;
            }
        }
        return null;
    }

    /** 当前登录用户 ID（未认证时返回 0L，表示系统）。 */
    public static Long getCurrentUserIdOrDefault() {
        Long userId = getCurrentUserId();
        return userId == null ? 0L : userId;
    }
}