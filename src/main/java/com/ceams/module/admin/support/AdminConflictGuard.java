package com.ceams.module.admin.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ceams.module.admin.entity.AdminMeetingApply;
import com.ceams.module.admin.entity.AdminVehicleApply;
import com.ceams.module.admin.enums.MeetingApplyStatus;
import com.ceams.module.admin.enums.VehicleApplyStatus;
import com.ceams.module.admin.mapper.AdminMeetingApplyMapper;
import com.ceams.module.admin.mapper.AdminVehicleApplyMapper;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 行政时段冲突校验：车辆与会议室统一口径 「existing.start_time &lt; new.end_time AND existing.end_time &gt; new.start_time」。
 *
 * <p>只统计在途 / 生效状态：用车为 APPROVING/APPROVED/USING，会议室为 APPROVING/APPROVED。
 * 审批通过时二次校验需排除自身记录，避免自身占用导致误判。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminConflictGuard {

    private final AdminVehicleApplyMapper vehicleApplyMapper;
    private final AdminMeetingApplyMapper meetingApplyMapper;

    /** 同一车辆在时段内是否已有在途 / 生效申请。 */
    public boolean hasVehicleConflict(
            Long vehicleId, LocalDateTime start, LocalDateTime end, Long excludeApplyId) {
        if (vehicleId == null || start == null || end == null) {
            return false;
        }
        LambdaQueryWrapper<AdminVehicleApply> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AdminVehicleApply::getVehicleId, vehicleId)
                .in(
                        AdminVehicleApply::getStatus,
                        VehicleApplyStatus.APPROVING,
                        VehicleApplyStatus.APPROVED,
                        VehicleApplyStatus.USING)
                .lt(AdminVehicleApply::getStartTime, end)
                .gt(AdminVehicleApply::getEndTime, start);
        if (excludeApplyId != null) {
            wrapper.ne(AdminVehicleApply::getId, excludeApplyId);
        }
        Long count = vehicleApplyMapper.selectCount(wrapper);
        return count != null && count > 0;
    }

    /** 同一会议室在时段内是否已有在途 / 生效预定。 */
    public boolean hasMeetingConflict(
            Long roomId, LocalDateTime start, LocalDateTime end, Long excludeApplyId) {
        if (roomId == null || start == null || end == null) {
            return false;
        }
        LambdaQueryWrapper<AdminMeetingApply> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AdminMeetingApply::getRoomId, roomId)
                .in(
                        AdminMeetingApply::getStatus,
                        MeetingApplyStatus.APPROVING,
                        MeetingApplyStatus.APPROVED)
                .lt(AdminMeetingApply::getStartTime, end)
                .gt(AdminMeetingApply::getEndTime, start);
        if (excludeApplyId != null) {
            wrapper.ne(AdminMeetingApply::getId, excludeApplyId);
        }
        Long count = meetingApplyMapper.selectCount(wrapper);
        return count != null && count > 0;
    }
}
