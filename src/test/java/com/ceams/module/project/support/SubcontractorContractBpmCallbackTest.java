package com.ceams.module.project.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.ceams.common.enums.ContractApprovalStatus;
import com.ceams.module.bpm.entity.BpmInstance;
import com.ceams.module.bpm.enums.BpmBizType;
import com.ceams.module.bpm.enums.BpmInstanceStatus;
import com.ceams.module.project.entity.SubcontractorContract;
import com.ceams.module.project.mapper.SubcontractorContractMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** 分包合同审批回调单元测试：通过/驳回状态流转与幂等。 */
@ExtendWith(MockitoExtension.class)
class SubcontractorContractBpmCallbackTest {

    private static final Long CONTRACT_ID = 7101L;

    @Mock private SubcontractorContractMapper contractMapper;

    private SubcontractorContractBpmCallback callback;

    @BeforeEach
    void setUp() {
        callback = new SubcontractorContractBpmCallback(contractMapper);
    }

    @Test
    void bizTypeShouldBeSubcontractContract() {
        assertThat(callback.bizType()).isEqualTo(BpmBizType.SUBCONTRACT_CONTRACT);
    }

    @Test
    void approvedShouldMarkApprovalApprovedOnly() {
        when(contractMapper.selectById(CONTRACT_ID))
                .thenReturn(contract(ContractApprovalStatus.PENDING));
        when(contractMapper.update(any(SubcontractorContract.class), any(Wrapper.class)))
                .thenReturn(1);

        callback.onInstanceFinished(instance(), BpmInstanceStatus.APPROVED);

        ArgumentCaptor<SubcontractorContract> captor =
                ArgumentCaptor.forClass(SubcontractorContract.class);
        verify(contractMapper).update(captor.capture(), any(Wrapper.class));
        assertThat(captor.getValue().getApprovalStatus())
                .isEqualTo(ContractApprovalStatus.APPROVED);
        assertThat(captor.getValue().getStatus()).isNull();
        assertThat(captor.getValue().getBpmInstanceId()).isNull();
        assertThat(captor.getValue().getVersion()).isNull();
    }

    @Test
    void terminatedShouldMarkApprovalRejected() {
        when(contractMapper.selectById(CONTRACT_ID))
                .thenReturn(contract(ContractApprovalStatus.PENDING));
        when(contractMapper.update(any(SubcontractorContract.class), any(Wrapper.class)))
                .thenReturn(1);

        callback.onInstanceFinished(instance(), BpmInstanceStatus.TERMINATED);

        ArgumentCaptor<SubcontractorContract> captor =
                ArgumentCaptor.forClass(SubcontractorContract.class);
        verify(contractMapper).update(captor.capture(), any(Wrapper.class));
        assertThat(captor.getValue().getApprovalStatus())
                .isEqualTo(ContractApprovalStatus.REJECTED);
    }

    @Test
    void repeatedCallbackShouldBeIdempotent() {
        when(contractMapper.selectById(CONTRACT_ID))
                .thenReturn(contract(ContractApprovalStatus.REJECTED));

        callback.onInstanceFinished(instance(), BpmInstanceStatus.TERMINATED);

        verify(contractMapper, never()).update(any(SubcontractorContract.class), any(Wrapper.class));
    }

    @Test
    void missingContractShouldSkipSafely() {
        when(contractMapper.selectById(CONTRACT_ID)).thenReturn(null);

        assertThatCode(() -> callback.onInstanceFinished(instance(), BpmInstanceStatus.APPROVED))
                .doesNotThrowAnyException();
    }

    private SubcontractorContract contract(ContractApprovalStatus status) {
        SubcontractorContract contract = new SubcontractorContract();
        contract.setId(CONTRACT_ID);
        contract.setContractNumber("FB-2026-001");
        contract.setApprovalStatus(status);
        return contract;
    }

    private BpmInstance instance() {
        BpmInstance instance = new BpmInstance();
        instance.setId(9711L);
        instance.setBizId(CONTRACT_ID);
        return instance;
    }
}
