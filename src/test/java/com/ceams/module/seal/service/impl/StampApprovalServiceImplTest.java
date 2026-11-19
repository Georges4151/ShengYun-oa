package com.ceams.module.seal.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.module.seal.converter.StampApprovalConverter;
import com.ceams.module.seal.dto.StampApprovalCreateRequest;
import com.ceams.module.seal.entity.ElectronicSeal;
import com.ceams.module.seal.entity.ElectronicSealApplication;
import com.ceams.module.seal.entity.StampApproval;
import com.ceams.module.seal.enums.ApplicationStatus;
import com.ceams.module.seal.enums.ApprovalStatus;
import com.ceams.module.seal.enums.SealStatus;
import com.ceams.module.seal.mapper.ElectronicSealApplicationMapper;
import com.ceams.module.seal.mapper.ElectronicSealMapper;
import com.ceams.module.seal.mapper.StampApprovalMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** {@link StampApprovalServiceImpl} 用印审批创建单元测试：重复待审批拦截与首次创建成功。 */
@ExtendWith(MockitoExtension.class)
class StampApprovalServiceImplTest {

    private static final Long APPLICATION_ID = 1001L;

    private static final Long SEAL_ID = 2001L;

    private static final Long APPROVAL_ID = 3001L;

    @Mock private StampApprovalMapper stampApprovalMapper;

    @Mock private ElectronicSealApplicationMapper electronicSealApplicationMapper;

    @Mock private ElectronicSealMapper electronicSealMapper;

    @Mock private StampApprovalConverter stampApprovalConverter;

    private StampApprovalServiceImpl service;

    @BeforeEach
    void setUp() {
        service =
                new StampApprovalServiceImpl(
                        stampApprovalMapper,
                        electronicSealApplicationMapper,
                        electronicSealMapper,
                        stampApprovalConverter);
    }

    @Test
    void createShouldRejectWhenPendingApprovalExists() {
        when(electronicSealApplicationMapper.selectByIdForUpdate(APPLICATION_ID))
                .thenReturn(pendingApplication());
        when(electronicSealMapper.selectById(SEAL_ID)).thenReturn(activeSeal());
        when(stampApprovalMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> service.create(createRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("该用印申请已存在待审批记录")
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.BIZ_STATE_CONFLICT);

        verify(stampApprovalMapper, never()).insert(any(StampApproval.class));
    }

    @Test
    void createShouldSucceedWhenNoPendingApprovalExists() {
        when(electronicSealApplicationMapper.selectByIdForUpdate(APPLICATION_ID))
                .thenReturn(pendingApplication());
        when(electronicSealMapper.selectById(SEAL_ID)).thenReturn(activeSeal());
        when(stampApprovalMapper.selectCount(any())).thenReturn(0L);
        when(stampApprovalMapper.insert(any(StampApproval.class)))
                .thenAnswer(
                        invocation -> {
                            StampApproval approval = invocation.getArgument(0);
                            approval.setId(APPROVAL_ID);
                            return 1;
                        });

        String id = service.create(createRequest());

        assertThat(id).isEqualTo(String.valueOf(APPROVAL_ID));
        ArgumentCaptor<StampApproval> captor = ArgumentCaptor.forClass(StampApproval.class);
        verify(stampApprovalMapper).insert(captor.capture());
        StampApproval inserted = captor.getValue();
        assertThat(inserted.getStampId()).isEqualTo(APPLICATION_ID);
        assertThat(inserted.getSealId()).isEqualTo(SEAL_ID);
        assertThat(inserted.getStatus()).isEqualTo(ApprovalStatus.PENDING);
    }

    private StampApprovalCreateRequest createRequest() {
        StampApprovalCreateRequest request = new StampApprovalCreateRequest();
        request.setStampId(APPLICATION_ID);
        request.setSealPermission("合同章使用");
        request.setStampPositions("第3页");
        return request;
    }

    private ElectronicSealApplication pendingApplication() {
        ElectronicSealApplication application = new ElectronicSealApplication();
        application.setId(APPLICATION_ID);
        application.setSealId(SEAL_ID);
        application.setStatus(ApplicationStatus.PENDING);
        application.setProjectName("测试项目");
        return application;
    }

    private ElectronicSeal activeSeal() {
        ElectronicSeal seal = new ElectronicSeal();
        seal.setId(SEAL_ID);
        seal.setSealName("合同专用章");
        seal.setSealCode("SEAL-001");
        seal.setStatus(SealStatus.ACTIVE);
        return seal;
    }
}
