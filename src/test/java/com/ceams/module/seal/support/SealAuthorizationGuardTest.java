package com.ceams.module.seal.support;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.module.seal.service.SealAuthorizationService;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/** 印章使用授权守卫单元测试：管理员/财务放行，普通用户按有效期授权判定。 */
@ExtendWith(MockitoExtension.class)
class SealAuthorizationGuardTest {

    private static final Long USER_ID = 20L;

    private static final Long SEAL_ID = 3001L;

    @Mock private SealAuthorizationService sealAuthorizationService;

    private SealAuthorizationGuard guard;

    @BeforeEach
    void setUp() {
        guard = new SealAuthorizationGuard(sealAuthorizationService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void 管理员直接放行且不查询授权() {
        authenticate("ROLE_SYS_ADMIN");

        assertThatCode(() -> guard.checkCanUse(SEAL_ID)).doesNotThrowAnyException();
        verifyNoInteractions(sealAuthorizationService);
    }

    @Test
    void 财务直接放行且不查询授权() {
        authenticate("ROLE_FINANCE");

        assertThatCode(() -> guard.checkCanUse(SEAL_ID)).doesNotThrowAnyException();
        verifyNoInteractions(sealAuthorizationService);
    }

    @Test
    void 普通用户无授权时拒绝() {
        authenticate("ROLE_STAFF");
        when(sealAuthorizationService.hasActiveAuthorization(SEAL_ID, USER_ID)).thenReturn(false);

        assertThatThrownBy(() -> guard.checkCanUse(SEAL_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.AUTH_ACCESS_DENIED);
    }

    @Test
    void 普通用户在有效期内放行() {
        authenticate("ROLE_STAFF");
        when(sealAuthorizationService.hasActiveAuthorization(SEAL_ID, USER_ID)).thenReturn(true);

        assertThatCode(() -> guard.checkCanUse(SEAL_ID)).doesNotThrowAnyException();
    }

    @Test
    void 普通用户授权已过期时拒绝() {
        authenticate("ROLE_STAFF");
        when(sealAuthorizationService.hasActiveAuthorization(SEAL_ID, USER_ID)).thenReturn(false);

        assertThatThrownBy(() -> guard.checkCanUse(SEAL_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("无权使用该印章");
    }

    private void authenticate(String... roles) {
        List<SimpleGrantedAuthority> authorities =
                Arrays.stream(roles).map(SimpleGrantedAuthority::new).toList();
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken(USER_ID, "n/a", authorities));
    }
}
