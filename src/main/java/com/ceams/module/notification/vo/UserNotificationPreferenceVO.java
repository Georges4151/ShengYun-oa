package com.ceams.module.notification.vo;

import com.ceams.module.notification.enums.NotificationPriority;
import com.ceams.module.notification.enums.NotificationType;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.time.LocalTime;
import lombok.Data;

/** 用户通知偏好视图对象。 */
@Data
public class UserNotificationPreferenceVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    private NotificationType notificationType;

    private Integer receiveSystem;

    private Integer receiveEmail;

    private Integer receiveSms;

    private Integer quietMode;

    private LocalTime quietStartTime;

    private LocalTime quietEndTime;

    private NotificationPriority priorityThreshold;
}
