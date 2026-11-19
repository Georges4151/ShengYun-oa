package com.ceams.module.notification.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ceams.common.base.BaseEntity;
import com.ceams.module.notification.enums.ContentType;
import com.ceams.module.notification.enums.NotificationPriority;
import com.ceams.module.notification.enums.NotificationStatus;
import com.ceams.module.notification.enums.NotificationType;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 通知（biz_notification）。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_notification")
public class Notification extends BaseEntity {

    /** 标题。 */
    private String title;

    /** 内容类型：TEXT/LINK。 */
    private ContentType contentType;

    /** 内容。 */
    private String content;

    /** 跳转链接。 */
    private String linkUrl;

    /** 优先级：LOW/NORMAL/HIGH/URGENT。 */
    private NotificationPriority priority = NotificationPriority.NORMAL;

    /** 通知类型（代码值）。 */
    private NotificationType notificationType;

    /** 发送人ID（逻辑引用 sys_user.id）。 */
    private Long senderId;

    /** 发送人姓名（冗余）。 */
    private String senderName;

    /** 生效开始时间。 */
    private LocalDateTime validStartTime;

    /** 生效结束时间。 */
    private LocalDateTime validEndTime;

    /** 状态：DRAFT/PUBLISHED/REVOKED。 */
    private NotificationStatus status = NotificationStatus.DRAFT;

    /** 发布时间。 */
    private LocalDateTime publishTime;
}
