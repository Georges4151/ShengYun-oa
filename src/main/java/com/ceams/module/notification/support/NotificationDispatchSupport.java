package com.ceams.module.notification.support;

import com.ceams.module.notification.dto.NotificationCreateRequest;
import com.ceams.module.notification.enums.ContentType;
import com.ceams.module.notification.enums.NotificationPriority;
import com.ceams.module.notification.enums.NotificationType;
import com.ceams.module.notification.service.NotificationService;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 业务事件站内通知统一发送组件。
 *
 * <p>各业务域（薪资、合同、资产、报修）的提醒文案与参数高度相似，统一收敛到本组件，避免重复代码。
 *
 * <p>事务策略：通知写入使用独立事务（{@code REQUIRES_NEW}），通知失败只回滚通知本身，
 * 不会把业务主事务标记为 rollback-only；调用方必须 {@code try/catch} 吞异常并记录 {@code warn}
 * 日志，保证通知失败不影响主业务（与 {@code BpmNotificationPublisher} 风格一致）。
 */
@Component
@RequiredArgsConstructor
public class NotificationDispatchSupport {

    /** 系统统一发送人名称。 */
    private static final String SYSTEM_SENDER = "系统";

    private final NotificationService notificationService;

    /**
     * 发送一条站内通知（独立事务）。
     *
     * @param type 通知类型
     * @param title 通知标题
     * @param content 通知内容
     * @param linkUrl 跳转深链，为空按纯文本通知处理
     * @param targetUserIds 接收人用户 ID 集合（去重、忽略空值）
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void send(
            NotificationType type,
            String title,
            String content,
            String linkUrl,
            Collection<Long> targetUserIds) {
        List<Long> targets =
                targetUserIds == null
                        ? List.of()
                        : targetUserIds.stream().filter(Objects::nonNull).distinct().toList();
        if (targets.isEmpty()) {
            return;
        }
        NotificationCreateRequest request = new NotificationCreateRequest();
        request.setTitle(title);
        request.setContent(content);
        request.setContentType(StringUtils.hasText(linkUrl) ? ContentType.LINK : ContentType.TEXT);
        request.setLinkUrl(linkUrl);
        request.setPriority(NotificationPriority.NORMAL);
        request.setNotificationType(type);
        request.setSenderName(SYSTEM_SENDER);
        notificationService.createAndPublishToUsers(request, targets);
    }
}
