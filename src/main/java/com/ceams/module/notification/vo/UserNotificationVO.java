package com.ceams.module.notification.vo;

import com.ceams.module.notification.enums.PushChannel;
import com.ceams.module.notification.enums.PushStatus;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户通知视图对象。
 */
@Data
public class UserNotificationVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long notificationId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    private Integer isRead;

    private LocalDateTime readTime;

    private PushChannel channel;

    private PushStatus pushStatus;

    private LocalDateTime pushTime;

    private LocalDateTime createTime;
}
