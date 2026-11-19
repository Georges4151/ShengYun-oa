package com.ceams.module.admin.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ceams.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 行政会议室（admin_meeting_room）。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("admin_meeting_room")
public class AdminMeetingRoom extends BaseEntity {

    /** 会议室名称（唯一）。 */
    private String name;

    /** 位置。 */
    private String location;

    /** 容纳人数。 */
    private Integer capacity;

    /** 设备（逗号分隔）。 */
    private String equipment;

    /** 状态：ENABLED/DISABLED。 */
    private String status;
}
