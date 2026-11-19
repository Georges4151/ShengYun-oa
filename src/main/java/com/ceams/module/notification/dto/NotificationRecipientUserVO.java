package com.ceams.module.notification.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

/** 通知接收用户（发布范围选择用，仅暴露最小字段）。 */
@Data
public class NotificationRecipientUserVO {

    /** 用户 ID（雪花 ID，序列化为字符串避免前端精度丢失）。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private String name;

    private String username;
}
