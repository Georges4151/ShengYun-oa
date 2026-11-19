package com.ceams.module.seal.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.ceams.module.seal.vo.SealStampResultVO;
import java.nio.file.Path;
import java.util.Base64;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** {@link SealStampServiceImpl#stamp} 审计 IP 写入测试：盖章记录与使用记录均应落客户端 IP。 */
@ExtendWith(MockitoExtension.class)
class SealStampAuditIpTest {

    private static final Long SEAL_ID = 3001L;

    private static final Long APPLICATION_ID = 4001L;

    private static final Long STAMP_ID = 5001L;

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
        // 落盘目录指向临时目录，避免真实文件污染工作区
        ReflectionTestUtils.setField(service, "stampDir", tempDir.toString());
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void stampShouldPersistClientIpFromForwardedHeader() {
        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        httpRequest.addHeader("X-Forwarded-For", "10.1.2.3, 10.0.0.1");
        httpRequest.setRemoteAddr("127.0.0.1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(httpRequest));

        when(electronicSealMapper.selectById(SEAL_ID)).thenReturn(activeSeal());
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

        SealStampResultVO result = service.stamp(stampRequest());

        ArgumentCaptor<ElectronicSealStamp> stampCaptor =
                ArgumentCaptor.forClass(ElectronicSealStamp.class);
        verify(electronicSealStampMapper).insert(stampCaptor.capture());
        assertThat(stampCaptor.getValue().getIpAddress()).isEqualTo("10.1.2.3");

        ArgumentCaptor<ElectronicSealUsage> usageCaptor =
                ArgumentCaptor.forClass(ElectronicSealUsage.class);
        verify(electronicSealUsageMapper).insert(usageCaptor.capture());
        assertThat(usageCaptor.getValue().getIpAddress()).isEqualTo("10.1.2.3");

        assertThat(result.getStampId()).isEqualTo(STAMP_ID);
    }

    private static SealStampRequest stampRequest() {
        SealStampRequest request = new SealStampRequest();
        request.setSealId(SEAL_ID);
        request.setApplicationId(APPLICATION_ID);
        request.setSourcePdfBase64(Base64.getEncoder().encodeToString("pdf".getBytes()));
        request.setSealImageBase64(Base64.getEncoder().encodeToString("image".getBytes()));
        return request;
    }

    private static ElectronicSeal activeSeal() {
        ElectronicSeal seal = new ElectronicSeal();
        seal.setId(SEAL_ID);
        seal.setSealName("合同章");
        seal.setSealImage("seal-image-path");
        seal.setStatus(SealStatus.ACTIVE);
        seal.setUsageCount(0);
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
