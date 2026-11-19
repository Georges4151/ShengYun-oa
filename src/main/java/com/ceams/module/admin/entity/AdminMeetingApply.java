package com.ceams.module.admin.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ceams.common.base.BaseEntity;
import com.ceams.module.admin.enums.MeetingApplyStatus;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 行政会议室预定（admin_meeting_apply）。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("admin_meeting_apply")
public class AdminMeetingApply extends BaseEntity {

    /** 会议室 ID（逻辑引用 admin_meeting_room.id）。 */
    private Long roomId;

    /** 申请人 ID（sys_user.id）。 */
    private Long applicantId;

    /** 会议主题。 */
    private String subject;

    /** 开始时间。 */
    private LocalDateTime startTime;

    /** 结束时间。 */
    private LocalDateTime endTime;

    /** 参会人 ID，逗号分隔。 */
    private String attendeeIds;

    /** BPM 实例 ID。 */
    private Long bpmInstanceId;

    /** 状态：APPROVING/APPROVED/REJECTED/CANCELLED。 */
    private MeetingApplyStatus status;
}
