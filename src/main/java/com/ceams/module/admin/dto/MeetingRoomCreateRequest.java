package com.ceams.module.admin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 会议室建档请求。 */
@Data
public class MeetingRoomCreateRequest {

    /** 会议室名称，唯一。 */
    @NotBlank(message = "会议室名称不能为空")
    private String name;

    /** 位置。 */
    private String location;

    /** 容纳人数。 */
    private Integer capacity;

    /** 设备（逗号分隔）。 */
    private String equipment;
}
