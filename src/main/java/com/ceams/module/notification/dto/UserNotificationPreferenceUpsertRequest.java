package com.ceams.module.notification.dto;

import com.ceams.module.notification.enums.NotificationPriority;
import com.ceams.module.notification.enums.NotificationType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalTime;

/**
 * 新建 / 更新用户通知偏好（按 (user_id, notification_type) 唯一键 upsert）。
 */
@Data
public class UserNotificationPreferenceUpsertRequest {

    @NotNull(message = "通知类型不能为空")
    private NotificationType notificationType;

    /** 接收站内信：0否 1是（缺省 1）。 */
    private Integer receiveSystem;

    /** 接收邮件：0否 1是（缺省 0）。 */
    private Integer receiveEmail;

    /** 接收短信：0否 1是（缺省 0）。 */
    private Integer receiveSms;

    /** 免打扰模式：0关闭 1开启（缺省 0）。 */
    private Integer quietMode;

    /** 免打扰开始时间。 */
    private LocalTime quietStartTime;

    /** 免打扰结束时间。 */
    private LocalTime quietEndTime;

    /** 优先级阈值：LOW/NORMAL/HIGH/URGENT。 */
    private NotificationPriority priorityThreshold;
}
