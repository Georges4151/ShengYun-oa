package com.ceams.module.notification.vo;

import com.ceams.module.notification.enums.NotificationPriority;
import com.ceams.module.notification.enums.NotificationType;
import com.ceams.module.notification.enums.PushChannel;
import com.ceams.module.notification.enums.PushStatus;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.time.LocalDateTime;
import lombok.Data;

/** 用户通知视图对象。 */
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

    /** 通知标题（展示冗余）。 */
    private String title;

    /** 通知内容（展示冗余）。 */
    private String content;

    /** 跳转链接（展示冗余）。 */
    private String linkUrl;

    /** 通知类型（展示冗余）。 */
    private NotificationType notificationType;

    /** 优先级（展示冗余）。 */
    private NotificationPriority priority;

    /** 通知发布时间（展示冗余）。 */
    private LocalDateTime publishTime;

    private LocalDateTime createTime;
}
