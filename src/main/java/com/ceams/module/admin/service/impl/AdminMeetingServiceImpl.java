package com.ceams.module.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.common.security.SecurityUtils;
import com.ceams.module.admin.dto.MeetingApplyRequest;
import com.ceams.module.admin.dto.MeetingRoomCreateRequest;
import com.ceams.module.admin.dto.MeetingRoomUpdateRequest;
import com.ceams.module.admin.entity.AdminMeetingApply;
import com.ceams.module.admin.entity.AdminMeetingRoom;
import com.ceams.module.admin.enums.MeetingApplyStatus;
import com.ceams.module.admin.mapper.AdminMeetingApplyMapper;
import com.ceams.module.admin.mapper.AdminMeetingRoomMapper;
import com.ceams.module.admin.service.AdminMeetingService;
import com.ceams.module.admin.support.AdminConflictGuard;
import com.ceams.module.admin.support.AdminConverter;
import com.ceams.module.admin.vo.MeetingApplyVO;
import com.ceams.module.admin.vo.MeetingAvailabilityVO;
import com.ceams.module.admin.vo.MeetingRoomVO;
import com.ceams.module.bpm.dto.InstanceStartRequest;
import com.ceams.module.bpm.enums.BpmBizType;
import com.ceams.module.bpm.service.BpmInstanceService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 行政会议室服务实现。
 *
 * <p>提交预定与审批通过两处都执行时段冲突校验，口径为
 * 「existing.start_time &lt; new.end_time AND existing.end_time &gt; new.start_time」，
 * 只统计 APPROVING/APPROVED 状态。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminMeetingServiceImpl implements AdminMeetingService {

    /** 会议室启用状态。 */
    private static final String ROOM_ENABLED = "ENABLED";

    private final AdminMeetingRoomMapper roomMapper;
    private final AdminMeetingApplyMapper applyMapper;
    private final AdminConflictGuard conflictGuard;
    private final BpmInstanceService bpmInstanceService;

    @Override
    public List<MeetingRoomVO> listRooms() {
        return roomMapper
                .selectList(
                        new LambdaQueryWrapper<AdminMeetingRoom>()
                                .orderByDesc(AdminMeetingRoom::getCreateTime))
                .stream()
                .map(AdminConverter::toMeetingRoomVO)
                .toList();
    }

    @Override
    @Transactional
    public String createRoom(MeetingRoomCreateRequest request) {
        String name = request.getName().trim();
        if (existsName(name, null)) {
            throw BusinessException.of(ErrorCode.BIZ_DUPLICATE_CODE, "会议室名称已存在");
        }
        requireNonNegative(request.getCapacity(), "容纳人数");

        AdminMeetingRoom room = new AdminMeetingRoom();
        room.setName(name);
        room.setLocation(request.getLocation());
        room.setCapacity(request.getCapacity());
        room.setEquipment(request.getEquipment());
        room.setStatus(ROOM_ENABLED);
        try {
            roomMapper.insert(room);
        } catch (DuplicateKeyException e) {
            throw BusinessException.of(ErrorCode.BIZ_DUPLICATE_CODE, "会议室名称已存在");
        }
        log.info("会议室建档成功：{}，名称 {}", room.getId(), name);
        return String.valueOf(room.getId());
    }

    @Override
    public void updateRoom(Long id, MeetingRoomUpdateRequest request) {
        AdminMeetingRoom room = requireRoom(id);
        if (StringUtils.isNotBlank(request.getName())) {
            String name = request.getName().trim();
            if (!name.equals(room.getName())) {
                if (existsName(name, id)) {
                    throw BusinessException.of(ErrorCode.BIZ_DUPLICATE_CODE, "会议室名称已存在");
                }
                room.setName(name);
            }
        }
        if (request.getLocation() != null) {
            room.setLocation(request.getLocation());
        }
        if (request.getCapacity() != null) {
            requireNonNegative(request.getCapacity(), "容纳人数");
            room.setCapacity(request.getCapacity());
        }
        if (request.getEquipment() != null) {
            room.setEquipment(request.getEquipment());
        }
        roomMapper.updateById(room);
        log.info("会议室 {} 基础信息已更新", id);
    }

    @Override
    @Transactional
    public String apply(MeetingApplyRequest request) {
        AdminMeetingRoom room = requireRoom(request.getRoomId());
        validateTimeRange(request.getStartTime(), request.getEndTime());
        List<Long> attendeeIds = distinctIds(request.getAttendeeIds());
        if (room.getCapacity() != null && attendeeIds.size() > room.getCapacity()) {
            throw BusinessException.of(ErrorCode.VALID_ILLEGAL_ARGUMENT, "参会人数超过会议室容量");
        }
        if (conflictGuard.hasMeetingConflict(
                request.getRoomId(), request.getStartTime(), request.getEndTime(), null)) {
            throw BusinessException.of(ErrorCode.BIZ_STATE_CONFLICT, "该会议室在所选时段已被占用，请调整时间");
        }

        AdminMeetingApply apply = new AdminMeetingApply();
        apply.setRoomId(request.getRoomId());
        apply.setApplicantId(SecurityUtils.getCurrentUserIdOrDefault());
        apply.setSubject(request.getSubject());
        apply.setStartTime(request.getStartTime());
        apply.setEndTime(request.getEndTime());
        apply.setAttendeeIds(AdminConverter.joinIds(attendeeIds));
        apply.setStatus(MeetingApplyStatus.APPROVING);
        applyMapper.insert(apply);

        InstanceStartRequest start = new InstanceStartRequest();
        start.setTemplateCode(BpmBizType.ADMIN_MEETING_APPLY.name());
        start.setBizType(BpmBizType.ADMIN_MEETING_APPLY.name());
        start.setBizId(apply.getId());
        Map<String, String> variables = new LinkedHashMap<>();
        variables.put("roomId", String.valueOf(request.getRoomId()));
        variables.put("roomName", room.getName());
        variables.put("applicantId", String.valueOf(apply.getApplicantId()));
        variables.put("startTime", request.getStartTime().toString());
        variables.put("endTime", request.getEndTime().toString());
        start.setVariables(variables);
        String instanceId = bpmInstanceService.start(start);

        apply.setBpmInstanceId(parseInstanceId(instanceId));
        applyMapper.updateById(apply);
        log.info(
                "会议室 {} 发起预定，预定单 {}，BPM 实例 {}",
                request.getRoomId(),
                apply.getId(),
                instanceId);
        return String.valueOf(apply.getId());
    }

    @Override
    public List<MeetingApplyVO> myApplies() {
        Long applicantId = SecurityUtils.getCurrentUserIdOrDefault();
        List<AdminMeetingApply> records =
                applyMapper.selectList(
                        new LambdaQueryWrapper<AdminMeetingApply>()
                                .eq(AdminMeetingApply::getApplicantId, applicantId)
                                .orderByDesc(AdminMeetingApply::getCreateTime));
        if (records.isEmpty()) {
            return List.of();
        }
        Map<Long, AdminMeetingRoom> rooms = loadRooms(records);
        return records.stream()
                .map(apply -> AdminConverter.toMeetingApplyVO(apply, rooms.get(apply.getRoomId())))
                .toList();
    }

    @Override
    public MeetingAvailabilityVO availability(Long roomId, LocalDate date) {
        if (roomId == null || date == null) {
            throw BusinessException.of(ErrorCode.VALID_ILLEGAL_ARGUMENT, "会议室与日期不能为空");
        }
        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd = dayStart.plusDays(1);
        List<AdminMeetingApply> records =
                applyMapper.selectList(
                        new LambdaQueryWrapper<AdminMeetingApply>()
                                .eq(AdminMeetingApply::getRoomId, roomId)
                                .in(
                                        AdminMeetingApply::getStatus,
                                        MeetingApplyStatus.APPROVING,
                                        MeetingApplyStatus.APPROVED)
                                .lt(AdminMeetingApply::getStartTime, dayEnd)
                                .gt(AdminMeetingApply::getEndTime, dayStart)
                                .orderByAsc(AdminMeetingApply::getStartTime));
        MeetingAvailabilityVO vo = new MeetingAvailabilityVO();
        vo.setRoomId(roomId);
        vo.setDate(date);
        vo.setIntervals(
                records.stream()
                        .sorted(Comparator.comparing(AdminMeetingApply::getStartTime))
                        .map(
                                record ->
                                        new MeetingAvailabilityVO.Interval(
                                                record.getStartTime(),
                                                record.getEndTime(),
                                                record.getSubject()))
                        .toList());
        return vo;
    }

    private AdminMeetingRoom requireRoom(Long id) {
        AdminMeetingRoom room = id == null ? null : roomMapper.selectById(id);
        if (room == null) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "会议室不存在");
        }
        return room;
    }

    private boolean existsName(String name, Long excludeId) {
        LambdaQueryWrapper<AdminMeetingRoom> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AdminMeetingRoom::getName, name);
        if (excludeId != null) {
            wrapper.ne(AdminMeetingRoom::getId, excludeId);
        }
        Long count = roomMapper.selectCount(wrapper);
        return count != null && count > 0;
    }

    private Map<Long, AdminMeetingRoom> loadRooms(List<AdminMeetingApply> records) {
        List<Long> roomIds =
                records.stream()
                        .map(AdminMeetingApply::getRoomId)
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList();
        if (roomIds.isEmpty()) {
            return Map.of();
        }
        return roomMapper.selectBatchIds(roomIds).stream()
                .collect(Collectors.toMap(AdminMeetingRoom::getId, Function.identity(), (a, b) -> a));
    }

    private List<Long> distinctIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return ids.stream().filter(Objects::nonNull).distinct().toList();
    }

    private void validateTimeRange(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            throw BusinessException.of(ErrorCode.VALID_ILLEGAL_ARGUMENT, "预定开始与结束时间不能为空");
        }
        if (!start.isBefore(end)) {
            throw BusinessException.of(ErrorCode.VALID_ILLEGAL_ARGUMENT, "预定开始时间必须早于结束时间");
        }
    }

    private void requireNonNegative(Integer value, String label) {
        if (value != null && value < 0) {
            throw BusinessException.of(ErrorCode.VALID_ILLEGAL_ARGUMENT, label + "不能为负数");
        }
    }

    private Long parseInstanceId(String instanceId) {
        if (StringUtils.isBlank(instanceId)) {
            return null;
        }
        try {
            return Long.valueOf(instanceId.trim());
        } catch (NumberFormatException e) {
            log.warn("BPM 实例 ID 非数字，无法回写会议室预定：{}", instanceId);
            return null;
        }
    }
}
