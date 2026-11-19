package com.ceams.module.seal.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.module.seal.converter.ElectronicSealApplicationConverter;
import com.ceams.module.seal.dto.ElectronicSealApplicationCreateRequest;
import com.ceams.module.seal.entity.ElectronicSeal;
import com.ceams.module.seal.entity.ElectronicSealApplication;
import com.ceams.module.seal.enums.SealStatus;
import com.ceams.module.seal.mapper.ElectronicSealApplicationMapper;
import com.ceams.module.seal.mapper.ElectronicSealMapper;
import com.ceams.module.seal.mapper.StampApprovalMapper;
import com.ceams.module.seal.service.SealAuthorizationService;
import com.ceams.module.seal.support.SealAuthorizationGuard;
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

/** 用印申请创建授权测试：普通用户无有效授权时拒绝，管理员直接放行。 */
@ExtendWith(MockitoExtension.class)
class ElectronicSealApplicationServiceImplTest {

    private static final Long USER_ID = 30L;

    private static final Long SEAL_ID = 4001L;

    private static final Long APPLICATION_ID = 5001L;

    @Mock private ElectronicSealApplicationMapper electronicSealApplicationMapper;
    @Mock private ElectronicSealMapper electronicSealMapper;
    @Mock private StampApprovalMapper stampApprovalMapper;
    @Mock private ElectronicSealApplicationConverter electronicSealApplicationConverter;
    @Mock private SealAuthorizationService sealAuthorizationService;

    private ElectronicSealApplicationServiceImpl service;

    @BeforeEach
    void setUp() {
        SealAuthorizationGuard guard = new SealAuthorizationGuard(sealAuthorizationService);
        service =
                new ElectronicSealApplicationServiceImpl(
                        electronicSealApplicationMapper,
                        electronicSealMapper,
                        stampApprovalMapper,
                        electronicSealApplicationConverter,
                        guard);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void 普通用户无有效授权时拒绝创建申请() {
        authenticate("ROLE_STAFF");
        when(electronicSealMapper.selectById(SEAL_ID)).thenReturn(activeSeal());
        when(sealAuthorizationService.hasActiveAuthorization(SEAL_ID, USER_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.create(createRequest()))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.AUTH_ACCESS_DENIED);

        verify(electronicSealApplicationMapper, never()).insert(any(ElectronicSealApplication.class));
    }

    @Test
    void 管理员创建申请时直接放行() {
        authenticate("ROLE_SYS_ADMIN");
        when(electronicSealMapper.selectById(SEAL_ID)).thenReturn(activeSeal());
        when(electronicSealApplicationMapper.insert(any(ElectronicSealApplication.class)))
                .thenAnswer(
                        invocation -> {
                            ElectronicSealApplication inserted = invocation.getArgument(0);
                            inserted.setId(APPLICATION_ID);
                            return 1;
                        });

        assertThat(service.create(createRequest())).isEqualTo(String.valueOf(APPLICATION_ID));
    }

    private void authenticate(String... roles) {
        List<SimpleGrantedAuthority> authorities =
                Arrays.stream(roles).map(SimpleGrantedAuthority::new).toList();
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken(USER_ID, "n/a", authorities));
    }

    private static ElectronicSealApplicationCreateRequest createRequest() {
        ElectronicSealApplicationCreateRequest request = new ElectronicSealApplicationCreateRequest();
        request.setSealId(SEAL_ID);
        request.setPurpose("合同签署");
        request.setApplicantName("张三");
        return request;
    }

    private static ElectronicSeal activeSeal() {
        ElectronicSeal seal = new ElectronicSeal();
        seal.setId(SEAL_ID);
        seal.setSealName("合同章");
        seal.setStatus(SealStatus.ACTIVE);
        return seal;
    }
}
