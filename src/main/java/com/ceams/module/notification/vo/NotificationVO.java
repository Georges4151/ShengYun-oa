package com.ceams.module.notification.vo;

import com.ceams.module.notification.enums.ContentType;
import com.ceams.module.notification.enums.NotificationPriority;
import com.ceams.module.notification.enums.NotificationStatus;
import com.ceams.module.notification.enums.NotificationType;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通知视图对象。
 */
@Data
public class NotificationVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private String title;

    private ContentType contentType;

    private String content;

    private String linkUrl;

    private NotificationPriority priority;

    private NotificationType notificationType;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long senderId;

    private String senderName;

    private LocalDateTime validStartTime;

    private LocalDateTime validEndTime;

    private NotificationStatus status;

    private LocalDateTime publishTime;

    private LocalDateTime createTime;
}
