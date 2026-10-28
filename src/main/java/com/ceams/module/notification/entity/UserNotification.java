package com.ceams.module.notification.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ceams.common.base.BaseEntity;
import com.ceams.module.notification.enums.PushChannel;
import com.ceams.module.notification.enums.PushStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 用户通知（biz_user_notification）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_user_notification")
public class UserNotification extends BaseEntity {

    /** 通知ID（逻辑引用 biz_notification.id）。 */
    private Long notificationId;

    /** 用户ID（逻辑引用 sys_user.id）。 */
    private Long userId;

    /** 是否已读：0未读 1已读。 */
    private Integer isRead = 0;

    /** 阅读时间。 */
    private LocalDateTime readTime;

    /** 推送渠道：SYSTEM/EMAIL/SMS。 */
    private PushChannel channel;

    /** 推送状态：PENDING/SENT/FAILED。 */
    private PushStatus pushStatus = PushStatus.PENDING;

    /** 推送时间。 */
    private LocalDateTime pushTime;
}
