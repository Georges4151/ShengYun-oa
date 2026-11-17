package com.ceams.module.admin.dto;

import lombok.Data;

/** 会议室编辑请求。 */
@Data
public class MeetingRoomUpdateRequest {

    /** 会议室名称。 */
    private String name;

    /** 位置。 */
    private String location;

    /** 容纳人数。 */
    private Integer capacity;

    /** 设备（逗号分隔）。 */
    private String equipment;
}
