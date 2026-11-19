package com.ceams.module.notification.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ceams.common.base.BaseEntity;
import com.ceams.module.notification.enums.NotificationPriority;
import com.ceams.module.notification.enums.NotificationType;
import java.time.LocalTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 用户通知偏好（biz_user_notification_preference）。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_user_notification_preference")
public class UserNotificationPreference extends BaseEntity {

    /** 用户ID（逻辑引用 sys_user.id）。 */
    private Long userId;

    /** 通知类型（代码值）。 */
    private NotificationType notificationType;

    /** 接收站内信：0否 1是。 */
    private Integer receiveSystem = 1;

    /** 接收邮件：0否 1是。 */
    private Integer receiveEmail = 0;

    /** 接收短信：0否 1是。 */
    private Integer receiveSms = 0;

    /** 免打扰模式：0关闭 1开启。 */
    private Integer quietMode = 0;

    /** 免打扰开始时间。 */
    private LocalTime quietStartTime;

    /** 免打扰结束时间。 */
    private LocalTime quietEndTime;

    /** 优先级阈值：LOW/NORMAL/HIGH/URGENT。 */
    private NotificationPriority priorityThreshold;
}
