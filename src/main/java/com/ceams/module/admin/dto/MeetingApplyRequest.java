package com.ceams.module.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

/** 会议室预定请求。 */
@Data
public class MeetingApplyRequest {

    /** 会议室 ID。 */
    @NotNull(message = "会议室 ID 不能为空")
    private Long roomId;

    /** 会议主题。 */
    @NotBlank(message = "会议主题不能为空")
    private String subject;

    /** 开始时间。 */
    @NotNull(message = "开始时间不能为空")
    private LocalDateTime startTime;

    /** 结束时间。 */
    @NotNull(message = "结束时间不能为空")
    private LocalDateTime endTime;

    /** 参会人 ID 列表。 */
    private List<Long> attendeeIds;
}
