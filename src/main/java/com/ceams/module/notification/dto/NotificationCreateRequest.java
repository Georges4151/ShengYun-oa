package com.ceams.module.notification.dto;

import com.ceams.module.notification.enums.ContentType;
import com.ceams.module.notification.enums.NotificationPriority;
import com.ceams.module.notification.enums.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 新建通知（状态固定为 DRAFT）。
 */
@Data
public class NotificationCreateRequest {

    @NotBlank(message = "标题不能为空")
    @Size(max = 255, message = "标题长度不能超过 255")
    private String title;

    /** 内容类型：TEXT/LINK。 */
    private ContentType contentType;

    private String content;

    @Size(max = 1000, message = "链接长度不能超过 1000")
    private String linkUrl;

    /** 优先级：LOW/NORMAL/HIGH/URGENT（缺省 NORMAL）。 */
    private NotificationPriority priority;

    @NotNull(message = "通知类型不能为空")
    private NotificationType notificationType;

    private Long senderId;

    @Size(max = 100, message = "发送人姓名长度不能超过 100")
    private String senderName;

    private LocalDateTime validStartTime;

    private LocalDateTime validEndTime;
}
