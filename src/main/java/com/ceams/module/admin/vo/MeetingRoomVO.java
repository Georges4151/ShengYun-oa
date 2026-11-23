package com.ceams.module.admin.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.time.LocalDateTime;
import lombok.Data;

/** 会议室展示对象。 */
@Data
public class MeetingRoomVO {

    /** 会议室 ID。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 名称。 */
    private String name;

    /** 位置。 */
    private String location;

    /** 容纳人数。 */
    private Integer capacity;

    /** 设备（逗号分隔）。 */
    private String equipment;

    /** 状态：ENABLED/DISABLED。 */
    private String status;

    /** 创建时间。 */
    private LocalDateTime createTime;
}
