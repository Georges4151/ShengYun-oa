package com.ceams.module.admin.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.module.admin.entity.AdminMeetingApply;
import com.ceams.module.admin.entity.AdminVehicle;
import com.ceams.module.admin.entity.AdminVehicleApply;
import com.ceams.module.admin.enums.MeetingApplyStatus;
import com.ceams.module.admin.enums.VehicleApplyStatus;
import com.ceams.module.admin.enums.VehicleStatus;
import com.ceams.module.admin.mapper.AdminMeetingApplyMapper;
import com.ceams.module.admin.mapper.AdminVehicleApplyMapper;
import com.ceams.module.admin.mapper.AdminVehicleMapper;
import com.ceams.module.bpm.entity.BpmInstance;
import com.ceams.module.bpm.enums.BpmInstanceStatus;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** 用车与会议室审批回调单元测试：通过 / 冲突 / 驳回 / 幂等。 */
@ExtendWith(MockitoExtension.class)
class AdminVehicleMeetingBpmCallbackTest {

    private static final Long APPLY_ID = 8101L;
    private static final Long VEHICLE_ID = 4001L;
    private static final Long ROOM_ID = 7001L;

    @Mock private AdminVehicleMapper vehicleMapper;
    @Mock private AdminVehicleApplyMapper vehicleApplyMapper;
    @Mock private AdminMeetingApplyMapper meetingApplyMapper;
    @Mock private AdminConflictGuard conflictGuard;

    private AdminVehicleBpmCallback vehicleCallback;
    private AdminMeetingBpmCallback meetingCallback;

    @BeforeEach
    void setUp() {
        vehicleCallback = new AdminVehicleBpmCallback(vehicleMapper, vehicleApplyMapper);
        meetingCallback = new AdminMeetingBpmCallback(meetingApplyMapper, conflictGuard);
    }

    @Test
    void vehicleApprovedShouldMarkApprovedAndVehicleUsing() {
        when(vehicleApplyMapper.selectById(APPLY_ID))
                .thenReturn(vehicleApply(VehicleApplyStatus.APPROVING));
        when(vehicleMapper.update(any(AdminVehicle.class), any(Wrapper.class))).thenReturn(1);
        when(vehicleApplyMapper.update(any(AdminVehicleApply.class), any(Wrapper.class)))
                .thenReturn(1);

        vehicleCallback.onInstanceFinished(instance(APPLY_ID), BpmInstanceStatus.APPROVED);

        ArgumentCaptor<AdminVehicle> vehicleCaptor =
                ArgumentCaptor.forClass(AdminVehicle.class);
        verify(vehicleMapper).update(vehicleCaptor.capture(), any(Wrapper.class));
        assertThat(vehicleCaptor.getValue().getStatus()).isEqualTo(VehicleStatus.USING);

        ArgumentCaptor<AdminVehicleApply> applyCaptor =
                ArgumentCaptor.forClass(AdminVehicleApply.class);
        verify(vehicleApplyMapper).update(applyCaptor.capture(), any(Wrapper.class));
        assertThat(applyCaptor.getValue().getStatus()).isEqualTo(VehicleApplyStatus.APPROVED);
    }

    @Test
    void vehicleApprovedShouldThrowConflictWhenVehicleNotIdle() {
        when(vehicleApplyMapper.selectById(APPLY_ID))
                .thenReturn(vehicleApply(VehicleApplyStatus.APPROVING));
        when(vehicleMapper.update(any(AdminVehicle.class), any(Wrapper.class))).thenReturn(0);

        assertThatThrownBy(
                        () ->
                                vehicleCallback.onInstanceFinished(
                                        instance(APPLY_ID), BpmInstanceStatus.APPROVED))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.BIZ_STATE_CONFLICT);
        verify(vehicleApplyMapper, never())
                .update(any(AdminVehicleApply.class), any(Wrapper.class));
    }

    @Test
    void vehicleRejectedShouldMarkRejectedAndKeepVehicle() {
        when(vehicleApplyMapper.selectById(APPLY_ID))
                .thenReturn(vehicleApply(VehicleApplyStatus.APPROVING));

        vehicleCallback.onInstanceFinished(instance(APPLY_ID), BpmInstanceStatus.WITHDRAWN);

        ArgumentCaptor<AdminVehicleApply> applyCaptor =
                ArgumentCaptor.forClass(AdminVehicleApply.class);
        verify(vehicleApplyMapper).update(applyCaptor.capture(), any(Wrapper.class));
        assertThat(applyCaptor.getValue().getStatus()).isEqualTo(VehicleApplyStatus.REJECTED);
        verifyNoInteractions(vehicleMapper);
    }

    @Test
    void vehicleCallbackShouldBeIdempotentWhenAlreadyApproved() {
        when(vehicleApplyMapper.selectById(APPLY_ID))
                .thenReturn(vehicleApply(VehicleApplyStatus.APPROVED));

        vehicleCallback.onInstanceFinished(instance(APPLY_ID), BpmInstanceStatus.APPROVED);

        verify(vehicleApplyMapper, never())
                .update(any(AdminVehicleApply.class), any(Wrapper.class));
        verifyNoInteractions(vehicleMapper);
    }

    @Test
    void meetingApprovedShouldThrowConflictWhenOverlapFoundOnSecondCheck() {
        when(meetingApplyMapper.selectById(APPLY_ID))
                .thenReturn(meetingApply(MeetingApplyStatus.APPROVING));
        when(conflictGuard.hasMeetingConflict(eq(ROOM_ID), any(), any(), eq(APPLY_ID)))
                .thenReturn(true);

        assertThatThrownBy(
                        () ->
                                meetingCallback.onInstanceFinished(
                                        instance(APPLY_ID), BpmInstanceStatus.APPROVED))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.BIZ_STATE_CONFLICT);
        verify(meetingApplyMapper, never())
                .update(any(AdminMeetingApply.class), any(Wrapper.class));
    }

    @Test
    void meetingApprovedShouldMarkApprovedExcludingSelf() {
        when(meetingApplyMapper.selectById(APPLY_ID))
                .thenReturn(meetingApply(MeetingApplyStatus.APPROVING));
        when(conflictGuard.hasMeetingConflict(eq(ROOM_ID), any(), any(), eq(APPLY_ID)))
                .thenReturn(false);
        when(meetingApplyMapper.update(any(AdminMeetingApply.class), any(Wrapper.class)))
                .thenReturn(1);

        meetingCallback.onInstanceFinished(instance(APPLY_ID), BpmInstanceStatus.APPROVED);

        verify(conflictGuard).hasMeetingConflict(eq(ROOM_ID), any(), any(), eq(APPLY_ID));
        ArgumentCaptor<AdminMeetingApply> captor =
                ArgumentCaptor.forClass(AdminMeetingApply.class);
        verify(meetingApplyMapper).update(captor.capture(), any(Wrapper.class));
        assertThat(captor.getValue().getStatus()).isEqualTo(MeetingApplyStatus.APPROVED);
    }

    @Test
    void meetingRejectedShouldMarkRejected() {
        when(meetingApplyMapper.selectById(APPLY_ID))
                .thenReturn(meetingApply(MeetingApplyStatus.APPROVING));

        meetingCallback.onInstanceFinished(instance(APPLY_ID), BpmInstanceStatus.REJECTED);

        ArgumentCaptor<AdminMeetingApply> captor =
                ArgumentCaptor.forClass(AdminMeetingApply.class);
        verify(meetingApplyMapper).update(captor.capture(), any(Wrapper.class));
        assertThat(captor.getValue().getStatus()).isEqualTo(MeetingApplyStatus.REJECTED);
        verifyNoInteractions(conflictGuard);
    }

    @Test
    void meetingCallbackShouldBeIdempotentWhenAlreadyApproved() {
        when(meetingApplyMapper.selectById(APPLY_ID))
                .thenReturn(meetingApply(MeetingApplyStatus.APPROVED));

        meetingCallback.onInstanceFinished(instance(APPLY_ID), BpmInstanceStatus.APPROVED);

        verify(meetingApplyMapper, never())
                .update(any(AdminMeetingApply.class), any(Wrapper.class));
        verifyNoInteractions(conflictGuard);
    }

    private AdminVehicleApply vehicleApply(VehicleApplyStatus status) {
        AdminVehicleApply apply = new AdminVehicleApply();
        apply.setId(APPLY_ID);
        apply.setVehicleId(VEHICLE_ID);
        apply.setApplicantId(0L);
        apply.setStatus(status);
        apply.setStartTime(LocalDateTime.of(2026, 11, 20, 9, 0));
        apply.setEndTime(LocalDateTime.of(2026, 11, 20, 12, 0));
        return apply;
    }

    private AdminMeetingApply meetingApply(MeetingApplyStatus status) {
        AdminMeetingApply apply = new AdminMeetingApply();
        apply.setId(APPLY_ID);
        apply.setRoomId(ROOM_ID);
        apply.setApplicantId(0L);
        apply.setSubject("方案评审");
        apply.setStatus(status);
        apply.setStartTime(LocalDateTime.of(2026, 11, 20, 9, 0));
        apply.setEndTime(LocalDateTime.of(2026, 11, 20, 10, 0));
        return apply;
    }

    private BpmInstance instance(Long bizId) {
        BpmInstance instance = new BpmInstance();
        instance.setId(9001L);
        instance.setBizId(bizId);
        return instance;
    }
}
