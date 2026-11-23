package com.ceams.module.hr.support;

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
import com.ceams.module.hr.entity.HrContract;
import com.ceams.module.hr.mapper.HrContractMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** 劳动合同审批回调单元测试：状态流转、只改审批快照、幂等。 */
@ExtendWith(MockitoExtension.class)
class HrContractBpmCallbackTest {

    private static final Long CONTRACT_ID = 7001L;

    @Mock private HrContractMapper contractMapper;

    private HrContractBpmCallback callback;

    @BeforeEach
    void setUp() {
        callback = new HrContractBpmCallback(contractMapper);
    }

    @Test
    void bizTypeShouldBeHrContract() {
        assertThat(callback.bizType()).isEqualTo(BpmBizType.HR_CONTRACT);
    }

    @Test
    void approvedShouldMarkApprovalApprovedOnly() {
        when(contractMapper.selectById(CONTRACT_ID))
                .thenReturn(contract(ContractApprovalStatus.PENDING));
        when(contractMapper.update(any(HrContract.class), any(Wrapper.class))).thenReturn(1);

        callback.onInstanceFinished(instance(), BpmInstanceStatus.APPROVED);

        ArgumentCaptor<HrContract> captor = ArgumentCaptor.forClass(HrContract.class);
        verify(contractMapper).update(captor.capture(), any(Wrapper.class));
        assertThat(captor.getValue().getApprovalStatus())
                .isEqualTo(ContractApprovalStatus.APPROVED);
        // 不改合同业务状态，不覆盖 bpm_instance_id
        assertThat(captor.getValue().getStatus()).isNull();
        assertThat(captor.getValue().getBpmInstanceId()).isNull();
        assertThat(captor.getValue().getVersion()).isNull();
    }

    @Test
    void rejectedAndWithdrawnAndTerminatedShouldMarkApprovalRejected() {
        for (BpmInstanceStatus status :
                new BpmInstanceStatus[] {
                    BpmInstanceStatus.REJECTED,
                    BpmInstanceStatus.WITHDRAWN,
                    BpmInstanceStatus.TERMINATED
                }) {
            HrContract contract = contract(ContractApprovalStatus.PENDING);
            when(contractMapper.selectById(CONTRACT_ID)).thenReturn(contract);
            when(contractMapper.update(any(HrContract.class), any(Wrapper.class))).thenReturn(1);

            callback.onInstanceFinished(instance(), status);

            ArgumentCaptor<HrContract> captor = ArgumentCaptor.forClass(HrContract.class);
            verify(contractMapper).update(captor.capture(), any(Wrapper.class));
            assertThat(captor.getValue().getApprovalStatus())
                    .isEqualTo(ContractApprovalStatus.REJECTED);
            // 便于下一轮断言使用全新的 mock 计数
            org.mockito.Mockito.clearInvocations(contractMapper);
        }
    }

    @Test
    void repeatedCallbackShouldBeIdempotent() {
        when(contractMapper.selectById(CONTRACT_ID))
                .thenReturn(contract(ContractApprovalStatus.APPROVED));

        callback.onInstanceFinished(instance(), BpmInstanceStatus.APPROVED);

        verify(contractMapper, never()).update(any(HrContract.class), any(Wrapper.class));
    }

    @Test
    void oldInstanceShouldNotOverwriteTerminalStatus() {
        when(contractMapper.selectById(CONTRACT_ID))
                .thenReturn(contract(ContractApprovalStatus.APPROVED));

        callback.onInstanceFinished(instance(), BpmInstanceStatus.REJECTED);

        verify(contractMapper, never()).update(any(HrContract.class), any(Wrapper.class));
    }

    @Test
    void runningStatusShouldBeIgnored() {
        callback.onInstanceFinished(instance(), BpmInstanceStatus.RUNNING);

        verify(contractMapper, never()).selectById(any());
        verify(contractMapper, never()).update(any(HrContract.class), any(Wrapper.class));
    }

    @Test
    void missingContractShouldSkipSafely() {
        when(contractMapper.selectById(CONTRACT_ID)).thenReturn(null);

        assertThatCode(() -> callback.onInstanceFinished(instance(), BpmInstanceStatus.APPROVED))
                .doesNotThrowAnyException();
    }

    private HrContract contract(ContractApprovalStatus status) {
        HrContract contract = new HrContract();
        contract.setId(CONTRACT_ID);
        contract.setContractNo("HT-2026-001");
        contract.setApprovalStatus(status);
        return contract;
    }

    private BpmInstance instance() {
        BpmInstance instance = new BpmInstance();
        instance.setId(9701L);
        instance.setBizId(CONTRACT_ID);
        return instance;
    }
}
