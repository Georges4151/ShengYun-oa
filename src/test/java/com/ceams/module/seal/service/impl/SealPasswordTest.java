package com.ceams.module.seal.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.common.pdf.PdfStampService;
import com.ceams.common.pdf.StampRequest;
import com.ceams.module.seal.dto.SealStampRequest;
import com.ceams.module.seal.entity.ElectronicSeal;
import com.ceams.module.seal.entity.ElectronicSealApplication;
import com.ceams.module.seal.entity.ElectronicSealStamp;
import com.ceams.module.seal.entity.ElectronicSealUsage;
import com.ceams.module.seal.enums.ApplicationStatus;
import com.ceams.module.seal.enums.SealStatus;
import com.ceams.module.seal.mapper.ElectronicSealApplicationMapper;
import com.ceams.module.seal.mapper.ElectronicSealMapper;
import com.ceams.module.seal.mapper.ElectronicSealStampMapper;
import com.ceams.module.seal.mapper.ElectronicSealUsageMapper;
import com.ceams.module.seal.support.SealAuthorizationGuard;
import java.nio.file.Path;
import java.util.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

/** 印章密码校验单元测试：启用保护时空密码/错误密码拒绝，正确密码放行并落库。 */
@ExtendWith(MockitoExtension.class)
class SealPasswordTest {

    private static final Long SEAL_ID = 6001L;

    private static final Long APPLICATION_ID = 7001L;

    private static final Long STAMP_ID = 8001L;

    private static final String PASSWORD_HASH = "$2a$10$abcdefghijklmnopqrstuv";

    @Mock private PdfStampService pdfStampService;
    @Mock private ElectronicSealMapper electronicSealMapper;
    @Mock private ElectronicSealApplicationMapper electronicSealApplicationMapper;
    @Mock private ElectronicSealStampMapper electronicSealStampMapper;
    @Mock private ElectronicSealUsageMapper electronicSealUsageMapper;
    @Mock private SealAuthorizationGuard sealAuthorizationGuard;
    @Mock private PasswordEncoder passwordEncoder;

    @TempDir private Path tempDir;

    private SealStampServiceImpl service;

    @BeforeEach
    void setUp() {
        service =
                new SealStampServiceImpl(
                        pdfStampService,
                        electronicSealMapper,
                        electronicSealApplicationMapper,
                        electronicSealStampMapper,
                        electronicSealUsageMapper,
                        sealAuthorizationGuard,
                        passwordEncoder);
        ReflectionTestUtils.setField(service, "stampDir", tempDir.toString());
    }

    @Test
    void 启用密码保护但未提供密码时应拒绝() {
        when(electronicSealMapper.selectById(SEAL_ID)).thenReturn(protectedSeal());

        assertThatThrownBy(() -> service.stamp(stampRequest(null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("该印章已启用密码保护，请输入印章密码")
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.VALID_ILLEGAL_ARGUMENT);
    }

    @Test
    void 印章密码错误时应拒绝() {
        when(electronicSealMapper.selectById(SEAL_ID)).thenReturn(protectedSeal());
        when(passwordEncoder.matches("wrong-password", PASSWORD_HASH)).thenReturn(false);

        assertThatThrownBy(() -> service.stamp(stampRequest("wrong-password")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("印章密码错误")
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.VALID_ILLEGAL_ARGUMENT);
    }

    @Test
    void 印章密码正确时应放行并落库() {
        when(electronicSealMapper.selectById(SEAL_ID)).thenReturn(protectedSeal());
        when(passwordEncoder.matches("right-password", PASSWORD_HASH)).thenReturn(true);
        when(electronicSealApplicationMapper.selectById(APPLICATION_ID))
                .thenReturn(approvedApplication());
        when(pdfStampService.stamp(any(byte[].class), any(StampRequest.class)))
                .thenReturn(new byte[] {1, 2, 3});
        when(electronicSealStampMapper.insert(any(ElectronicSealStamp.class)))
                .thenAnswer(
                        invocation -> {
                            ElectronicSealStamp inserted =
                                    invocation.getArgument(0, ElectronicSealStamp.class);
                            inserted.setId(STAMP_ID);
                            return 1;
                        });

        assertThat(service.stamp(stampRequest("right-password")).getStampId())
                .isEqualTo(STAMP_ID);
        verify(electronicSealStampMapper).insert(any(ElectronicSealStamp.class));
        verify(electronicSealUsageMapper).insert(any(ElectronicSealUsage.class));
    }

    private static SealStampRequest stampRequest(String sealPassword) {
        SealStampRequest request = new SealStampRequest();
        request.setSealId(SEAL_ID);
        request.setApplicationId(APPLICATION_ID);
        request.setSourcePdfBase64(Base64.getEncoder().encodeToString("pdf".getBytes()));
        request.setSealImageBase64(Base64.getEncoder().encodeToString("image".getBytes()));
        request.setSealPassword(sealPassword);
        return request;
    }

    private static ElectronicSeal protectedSeal() {
        ElectronicSeal seal = new ElectronicSeal();
        seal.setId(SEAL_ID);
        seal.setSealName("合同章");
        seal.setSealImage("seal-image-path");
        seal.setStatus(SealStatus.ACTIVE);
        seal.setUsageCount(0);
        seal.setPasswordProtected(Boolean.TRUE);
        seal.setPasswordHash(PASSWORD_HASH);
        return seal;
    }

    private static ElectronicSealApplication approvedApplication() {
        ElectronicSealApplication application = new ElectronicSealApplication();
        application.setId(APPLICATION_ID);
        application.setSealId(SEAL_ID);
        application.setStatus(ApplicationStatus.APPROVED);
        application.setPurpose("合同签署");
        application.setProjectId(42L);
        application.setProjectName("测试项目");
        application.setContractName("测试合同");
        application.setContractPath("/contracts/test.pdf");
        application.setStampPosition("10,20");
        return application;
    }
}
