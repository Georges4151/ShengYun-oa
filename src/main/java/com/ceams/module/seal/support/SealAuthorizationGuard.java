package com.ceams.module.seal.support;

import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.common.security.SecurityUtils;
import com.ceams.module.seal.service.SealAuthorizationService;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * 印章使用授权守卫：管理员/财务直接放行，其他用户需命中有效期内授权。
 *
 * <p>角色来源与 {@code JwtAuthenticationFilter} 保持一致：authorities 为 {@code ROLE_<角色>}。
 * 未认证或未授权时抛出 403，避免仅依赖控制器注解导致越权。
 */
@Component
@RequiredArgsConstructor
public class SealAuthorizationGuard {

    private static final String ROLE_PREFIX = "ROLE_";

    /** 可直接使用任意印章的角色（不含 ROLE_ 前缀）。 */
    private static final List<String> PRIVILEGED_ROLES = List.of("SYS_ADMIN", "FINANCE");

    private final SealAuthorizationService sealAuthorizationService;

    /** 校验当前用户是否有权使用指定印章，无权时抛出 403。 */
    public void checkCanUse(Long sealId) {
        if (hasAnyRole(PRIVILEGED_ROLES)) {
            return;
        }
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null || !sealAuthorizationService.hasActiveAuthorization(sealId, userId)) {
            throw BusinessException.of(ErrorCode.AUTH_ACCESS_DENIED, "无权使用该印章，请联系管理员授权");
        }
    }

    private boolean hasAnyRole(List<String> roles) {
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
