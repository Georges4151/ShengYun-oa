package com.ceams.module.notification.dto;

import com.ceams.module.notification.enums.ContentType;
import com.ceams.module.notification.enums.NotificationPriority;
import com.ceams.module.notification.enums.NotificationType;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import lombok.Data;

/** 更新通知（仅 DRAFT 可编辑，仅更新非空字段）。 */
@Data
public class NotificationUpdateRequest {

    @Size(max = 255, message = "标题长度不能超过 255")
    private String title;

    /** 内容类型：TEXT/LINK。 */
    private ContentType contentType;

    private String content;

    @Size(max = 1000, message = "链接长度不能超过 1000")
    private String linkUrl;

    /** 优先级：LOW/NORMAL/HIGH/URGENT。 */
    private NotificationPriority priority;

    /** 通知类型（代码值）。 */
    private NotificationType notificationType;

    private Long senderId;

    @Size(max = 100, message = "发送人姓名长度不能超过 100")
    private String senderName;

    private LocalDateTime validStartTime;

    private LocalDateTime validEndTime;
}
