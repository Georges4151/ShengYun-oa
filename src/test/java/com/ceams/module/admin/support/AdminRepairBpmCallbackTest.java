package com.ceams.module.admin.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.module.admin.entity.AdminRepair;
import com.ceams.module.admin.enums.RepairStatus;
import com.ceams.module.admin.mapper.AdminRepairMapper;
import com.ceams.module.bpm.entity.BpmInstance;
import com.ceams.module.bpm.enums.BpmBizType;
import com.ceams.module.bpm.enums.BpmInstanceStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** 报修审批回调单元测试：通过置 ASSIGNED、驳回置 REJECTED、幂等与冲突。 */
@ExtendWith(MockitoExtension.class)
class AdminRepairBpmCallbackTest {

    private static final Long REPAIR_ID = 3201L;

    @Mock private AdminRepairMapper repairMapper;

    private AdminRepairBpmCallback callback;

    @BeforeEach
    void setUp() {
        callback = new AdminRepairBpmCallback(repairMapper);
    }

    @Test
    void bizTypeShouldBeAdminRepair() {
        assertThat(callback.bizType()).isEqualTo(BpmBizType.ADMIN_REPAIR);
    }

    @Test
    void approvedShouldMarkAssignedWithAssignedAt() {
        when(repairMapper.selectById(REPAIR_ID)).thenReturn(repair(RepairStatus.SUBMITTED));
        when(repairMapper.update(any(AdminRepair.class), any(Wrapper.class))).thenReturn(1);

        callback.onInstanceFinished(instance(REPAIR_ID), BpmInstanceStatus.APPROVED);

        ArgumentCaptor<AdminRepair> captor = ArgumentCaptor.forClass(AdminRepair.class);
        verify(repairMapper).update(captor.capture(), any(Wrapper.class));
        assertThat(captor.getValue().getStatus()).isEqualTo(RepairStatus.ASSIGNED);
        assertThat(captor.getValue().getAssignedAt()).isNotNull();
        // 维修人留空，等待行政派单
        assertThat(captor.getValue().getAssigneeId()).isNull();
        assertThat(captor.getValue().getVersion()).isNull();
    }

    @Test
    void approvedShouldThrowConflictWhenStatusChanged() {
        when(repairMapper.selectById(REPAIR_ID)).thenReturn(repair(RepairStatus.SUBMITTED));
        when(repairMapper.update(any(AdminRepair.class), any(Wrapper.class))).thenReturn(0);

        assertThatThrownBy(
                        () ->
                                callback.onInstanceFinished(
                                        instance(REPAIR_ID), BpmInstanceStatus.APPROVED))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.BIZ_STATE_CONFLICT);
    }

    @Test
    void approvedShouldBeIdempotentWhenAlreadyProcessing() {
        when(repairMapper.selectById(REPAIR_ID)).thenReturn(repair(RepairStatus.PROCESSING));

        callback.onInstanceFinished(instance(REPAIR_ID), BpmInstanceStatus.APPROVED);

        verify(repairMapper, never()).update(any(AdminRepair.class), any(Wrapper.class));
    }

    @Test
    void rejectedShouldMarkRejected() {
        when(repairMapper.selectById(REPAIR_ID)).thenReturn(repair(RepairStatus.SUBMITTED));

        callback.onInstanceFinished(instance(REPAIR_ID), BpmInstanceStatus.REJECTED);

        ArgumentCaptor<AdminRepair> captor = ArgumentCaptor.forClass(AdminRepair.class);
        verify(repairMapper).update(captor.capture(), any(Wrapper.class));
        assertThat(captor.getValue().getStatus()).isEqualTo(RepairStatus.REJECTED);
    }

    @Test
    void rejectedShouldBeIdempotentWhenAlreadyRejected() {
        when(repairMapper.selectById(REPAIR_ID)).thenReturn(repair(RepairStatus.REJECTED));

        callback.onInstanceFinished(instance(REPAIR_ID), BpmInstanceStatus.TERMINATED);

        verify(repairMapper, never()).update(any(AdminRepair.class), any(Wrapper.class));
    }

    private AdminRepair repair(RepairStatus status) {
        AdminRepair repair = new AdminRepair();
        repair.setId(REPAIR_ID);
        repair.setRepairNo("RP-20261114-0002");
        repair.setReporterId(0L);
        repair.setStatus(status);
        return repair;
    }

    private BpmInstance instance(Long bizId) {
        BpmInstance instance = new BpmInstance();
        instance.setId(9401L);
        instance.setBizId(bizId);
        return instance;
    }
}
