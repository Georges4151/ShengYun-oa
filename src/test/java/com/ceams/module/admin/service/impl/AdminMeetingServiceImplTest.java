package com.ceams.module.admin.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.module.admin.dto.MeetingApplyRequest;
import com.ceams.module.admin.entity.AdminMeetingApply;
import com.ceams.module.admin.entity.AdminMeetingRoom;
import com.ceams.module.admin.enums.MeetingApplyStatus;
import com.ceams.module.admin.mapper.AdminMeetingApplyMapper;
import com.ceams.module.admin.mapper.AdminMeetingRoomMapper;
import com.ceams.module.admin.support.AdminConflictGuard;
import com.ceams.module.admin.vo.MeetingAvailabilityVO;
import com.ceams.module.bpm.dto.InstanceStartRequest;
import com.ceams.module.bpm.service.BpmInstanceService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** 会议室服务单元测试：时间校验、容量校验、冲突校验与可用区间查询。 */
@ExtendWith(MockitoExtension.class)
class AdminMeetingServiceImplTest {

    private static final Long ROOM_ID = 7001L;
    private static final Long APPLY_ID = 7101L;

    @Mock private AdminMeetingRoomMapper roomMapper;
    @Mock private AdminMeetingApplyMapper applyMapper;
    @Mock private AdminConflictGuard conflictGuard;
    @Mock private BpmInstanceService bpmInstanceService;

    private AdminMeetingServiceImpl meetingService;

    @BeforeEach
    void setUp() {
        meetingService =
                new AdminMeetingServiceImpl(roomMapper, applyMapper, conflictGuard, bpmInstanceService);
    }

    @Test
    void applyShouldRejectWhenTimeRangeInvalid() {
        when(roomMapper.selectById(ROOM_ID)).thenReturn(room(10));
        LocalDateTime start = LocalDateTime.of(2026, 11, 20, 9, 0);

        assertThatThrownBy(() -> meetingService.apply(request(start, start, List.of(1L))))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.VALID_ILLEGAL_ARGUMENT);
        verify(applyMapper, never()).insert(any(AdminMeetingApply.class));
    }

    @Test
    void applyShouldRejectWhenAttendeesExceedCapacity() {
        when(roomMapper.selectById(ROOM_ID)).thenReturn(room(3));

        assertThatThrownBy(
                        () ->
                                meetingService.apply(
                                        request(
                                                LocalDateTime.of(2026, 11, 20, 9, 0),
                                                LocalDateTime.of(2026, 11, 20, 10, 0),
                                                List.of(1L, 2L, 3L, 4L))))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.VALID_ILLEGAL_ARGUMENT);
        verify(applyMapper, never()).insert(any(AdminMeetingApply.class));
    }

    @Test
    void applyShouldRejectWhenRoomAlreadyOccupied() {
        when(roomMapper.selectById(ROOM_ID)).thenReturn(room(10));
        when(conflictGuard.hasMeetingConflict(eq(ROOM_ID), any(), any(), isNull()))
                .thenReturn(true);

        assertThatThrownBy(
                        () ->
                                meetingService.apply(
                                        request(
                                                LocalDateTime.of(2026, 11, 20, 9, 0),
                                                LocalDateTime.of(2026, 11, 20, 10, 0),
                                                List.of(1L))))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.BIZ_STATE_CONFLICT);
        verify(applyMapper, never()).insert(any(AdminMeetingApply.class));
        verify(bpmInstanceService, never()).start(any(InstanceStartRequest.class));
    }

    @Test
    void applyShouldCreateApprovingRecordWithDedupedAttendeesAndStartBpm() {
        when(roomMapper.selectById(ROOM_ID)).thenReturn(room(10));
        when(conflictGuard.hasMeetingConflict(eq(ROOM_ID), any(), any(), isNull()))
                .thenReturn(false);
        when(applyMapper.insert(any(AdminMeetingApply.class)))
                .thenAnswer(
                        invocation -> {
                            AdminMeetingApply record = invocation.getArgument(0);
                            record.setId(APPLY_ID);
                            return 1;
                        });
        when(bpmInstanceService.start(any(InstanceStartRequest.class))).thenReturn("9200");

        String id =
                meetingService.apply(
                        request(
                                LocalDateTime.of(2026, 11, 20, 9, 0),
                                LocalDateTime.of(2026, 11, 20, 10, 0),
                                List.of(1L, 2L, 2L)));

        assertThat(id).isEqualTo(String.valueOf(APPLY_ID));
        ArgumentCaptor<AdminMeetingApply> insertCaptor =
                ArgumentCaptor.forClass(AdminMeetingApply.class);
        verify(applyMapper).insert(insertCaptor.capture());
        assertThat(insertCaptor.getValue().getStatus()).isEqualTo(MeetingApplyStatus.APPROVING);
        assertThat(insertCaptor.getValue().getAttendeeIds()).isEqualTo("1,2");
        assertThat(insertCaptor.getValue().getApplicantId()).isEqualTo(0L);

        ArgumentCaptor<InstanceStartRequest> startCaptor =
                ArgumentCaptor.forClass(InstanceStartRequest.class);
        verify(bpmInstanceService).start(startCaptor.capture());
        assertThat(startCaptor.getValue().getTemplateCode()).isEqualTo("ADMIN_MEETING_APPLY");
        assertThat(startCaptor.getValue().getBizId()).isEqualTo(APPLY_ID);

        ArgumentCaptor<AdminMeetingApply> updateCaptor =
                ArgumentCaptor.forClass(AdminMeetingApply.class);
        verify(applyMapper).updateById(updateCaptor.capture());
        assertThat(updateCaptor.getValue().getBpmInstanceId()).isEqualTo(9200L);
    }

    @Test
    void availabilityShouldReturnOccupiedIntervalsInStartTimeOrder() {
        LocalDate date = LocalDate.of(2026, 11, 20);
        AdminMeetingApply later = meetingApply(LocalDateTime.of(2026, 11, 20, 14, 0), "周会");
        later.setId(APPLY_ID + 1);
        AdminMeetingApply earlier = meetingApply(LocalDateTime.of(2026, 11, 20, 9, 0), "评审");
        // 故意乱序返回，验证服务侧按开始时间升序输出
        when(applyMapper.selectList(any(Wrapper.class))).thenReturn(List.of(later, earlier));

        MeetingAvailabilityVO vo = meetingService.availability(ROOM_ID, date);

        assertThat(vo.getRoomId()).isEqualTo(ROOM_ID);
        assertThat(vo.getDate()).isEqualTo(date);
        assertThat(vo.getIntervals()).hasSize(2);
        assertThat(vo.getIntervals().get(0).getStartTime())
                .isEqualTo(LocalDateTime.of(2026, 11, 20, 9, 0));
        assertThat(vo.getIntervals().get(0).getSubject()).isEqualTo("评审");
        assertThat(vo.getIntervals().get(1).getStartTime())
                .isEqualTo(LocalDateTime.of(2026, 11, 20, 14, 0));
    }

    @Test
    void availabilityShouldRejectWhenDateMissing() {
        assertThatThrownBy(() -> meetingService.availability(ROOM_ID, null))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.VALID_ILLEGAL_ARGUMENT);
        verify(applyMapper, never()).selectList(any(Wrapper.class));
    }

    private AdminMeetingRoom room(Integer capacity) {
        AdminMeetingRoom room = new AdminMeetingRoom();
        room.setId(ROOM_ID);
        room.setName("第一会议室");
        room.setCapacity(capacity);
        room.setStatus("ENABLED");
        return room;
    }

    private AdminMeetingApply meetingApply(LocalDateTime start, String subject) {
        AdminMeetingApply apply = new AdminMeetingApply();
        apply.setId(APPLY_ID);
        apply.setRoomId(ROOM_ID);
        apply.setApplicantId(0L);
        apply.setSubject(subject);
        apply.setStartTime(start);
        apply.setEndTime(start.plusHours(1));
        apply.setStatus(MeetingApplyStatus.APPROVING);
        return apply;
    }

    private MeetingApplyRequest request(
            LocalDateTime start, LocalDateTime end, List<Long> attendeeIds) {
        MeetingApplyRequest request = new MeetingApplyRequest();
        request.setRoomId(ROOM_ID);
        request.setSubject("方案评审");
        request.setStartTime(start);
        request.setEndTime(end);
        request.setAttendeeIds(attendeeIds);
        return request;
    }
}
