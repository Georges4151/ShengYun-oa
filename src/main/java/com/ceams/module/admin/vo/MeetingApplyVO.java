package com.ceams.module.admin.vo;

import com.ceams.module.admin.enums.MeetingApplyStatus;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

/** 会议室预定展示对象。 */
@Data
public class MeetingApplyVO {

    /** 预定单 ID。 */
    private Long id;

    /** 会议室 ID。 */
    private Long roomId;

    /** 会议室名称。 */
    private String roomName;

    /** 申请人 ID。 */
    private Long applicantId;

    /** 会议主题。 */
    private String subject;

    /** 开始时间。 */
    private LocalDateTime startTime;

    /** 结束时间。 */
    private LocalDateTime endTime;

    /** 参会人 ID 列表。 */
    private List<Long> attendeeIds;

    /** BPM 实例 ID。 */
    private Long bpmInstanceId;

    /** 状态。 */
    private MeetingApplyStatus status;

    /** 创建时间。 */
    private LocalDateTime createTime;
}
