package com.ceams.module.bpm.support;

import com.ceams.module.bpm.entity.BpmCcRecord;
import com.ceams.module.bpm.entity.BpmInstance;
import com.ceams.module.bpm.entity.BpmTask;
import com.ceams.module.bpm.enums.BpmInstanceStatus;
import com.ceams.module.integration.support.WeComMessageOutbox;
import com.ceams.module.notification.dto.NotificationCreateRequest;
import com.ceams.module.notification.enums.ContentType;
import com.ceams.module.notification.enums.NotificationPriority;
import com.ceams.module.notification.enums.NotificationType;
import com.ceams.module.notification.service.NotificationService;
import com.ceams.module.notification.wecom.WeComPushClient;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

/**
 * BPM 站内通知发布器。
 *
 * <p>统一封装审批相关站内通知（待办生成、任务改派、超时提醒、抄送、实例结束），调用通知模块的
 * 群发接口。为避免通知失败影响审批主流程：
 *
 * <ol>
 *   <li>所有发送逻辑都在 {@code try/catch} 中执行，异常仅记录 {@code warn} 日志后吞掉；
 *   <li>通知写入使用独立事务（{@code PROPAGATION_REQUIRES_NEW}），通知失败只回滚通知本身，
 *       不会把审批事务标记为回滚，也不会因为通知异常导致审批接口失败。
 * </ol>
 *
 * <p>注意：通知发送在审批事务提交前完成，若审批事务最终回滚，通知可能已落库（属可接受的最终一致
 * 取舍，仅影响提示文案，不影响审批数据正确性）。
 */
@Slf4j
@Component
public class BpmNotificationPublisher {

    /** 待办中心深链前缀：Web 与桌面端共用同一套 ceams:// 协议解析 */
    private static final String DEEP_LINK_TODO = "ceams://todo";

    private final NotificationService notificationService;

    private final WeComPushClient weComPushClient;

    private final WeComMessageOutbox weComMessageOutbox;

    private final TransactionTemplate notificationTransactionTemplate;

    public BpmNotificationPublisher(
            NotificationService notificationService,
            WeComPushClient weComPushClient,
            WeComMessageOutbox weComMessageOutbox,
            PlatformTransactionManager transactionManager) {
        this.notificationService = notificationService;
        this.weComPushClient = weComPushClient;
        this.weComMessageOutbox = weComMessageOutbox;
        this.notificationTransactionTemplate = new TransactionTemplate(transactionManager);
        // 通知使用独立事务：通知失败不会污染审批主事务的 rollback-only 标记
        this.notificationTransactionTemplate.setPropagationBehavior(
                TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /** 待办生成：通知任务处理人。 */
    public void onTaskCreated(BpmInstance instance, BpmTask task) {
        if (task == null || task.getAssigneeId() == null) {
            return;
        }
        String title = "审批待办提醒（" + bizType(instance) + "）";
        String content =
                "您有一条新的审批待办：业务类型="
                        + bizType(instance)
                        + "，业务ID="
                        + bizId(instance)
                        + "，任务ID="
                        + task.getId()
                        + "。";
        send(title, content, taskLink(task.getId(), instanceId(instance)), List.of(task.getAssigneeId()));
    }

    /** 任务改派：通知新处理人。 */
    public void onTaskDelegated(BpmInstance instance, BpmTask task) {
        if (task == null || task.getAssigneeId() == null) {
            return;
        }
        String title = "审批任务改派提醒（" + bizType(instance) + "）";
        String content =
                "有一条审批任务已改派给您：业务类型="
                        + bizType(instance)
                        + "，业务ID="
                        + bizId(instance)
                        + "，任务ID="
                        + task.getId()
                        + "。";
        send(title, content, taskLink(task.getId(), instanceId(instance)), List.of(task.getAssigneeId()));
    }

    /** 超时提醒：通知超时任务的处理人（同一任务仅提醒一次，由调用方保证）。 */
    public void onTaskOverdue(BpmInstance instance, BpmTask task) {
        if (task == null || task.getAssigneeId() == null) {
            return;
        }
        String title = "审批任务超时提醒（" + bizType(instance) + "）";
        String content =
                "您的审批任务已超时：业务类型="
                        + bizType(instance)
                        + "，业务ID="
                        + bizId(instance)
                        + "，任务ID="
                        + task.getId()
                        + "，超时时间="
                        + task.getDueAt()
                        + "。";
        send(title, content, taskLink(task.getId(), instanceId(instance)), List.of(task.getAssigneeId()));
        log.info("BPM 超时提醒已发送：taskId={}，assigneeId={}", task.getId(), task.getAssigneeId());
    }

    /** 抄送：通知抄送接收人。 */
    public void onCcCreated(BpmInstance instance, BpmCcRecord record) {
        if (record == null || record.getReceiverId() == null) {
            return;
        }
        String title = "流程抄送提醒（" + bizType(instance) + "）";
        String content =
                "您收到一条流程抄送：业务类型="
                        + bizType(instance)
                        + "，业务ID="
                        + bizId(instance)
                        + "，流程实例ID="
                        + instanceId(instance)
                        + "。";
        send(title, content, ccLink(instance), List.of(record.getReceiverId()));
    }

    /** 实例结束：通知发起人（APPROVED/REJECTED/WITHDRAWN/TERMINATED）。 */
    public void onInstanceFinished(BpmInstance instance, BpmInstanceStatus status) {
        if (instance == null || instance.getInitiatorId() == null || status == null) {
            return;
        }
        String action = finishAction(status);
        if (action == null) {
            return;
        }
        String title = action + "（" + bizType(instance) + "）";
        String content =
                "您发起的流程"
                        + action
                        + "：业务类型="
                        + bizType(instance)
                        + "，业务ID="
                        + bizId(instance)
                        + "，流程实例ID="
                        + instanceId(instance)
                        + "。";
        send(title, content, instanceLink(instance), List.of(instance.getInitiatorId()));
    }

    /**
     * 统一发送入口：去重目标用户、独立事务写入通知，异常降级为告警日志。
     *
     * @param title 通知标题
     * @param content 通知内容
     * @param linkUrl 跳转链接（为空时按纯文本通知）
     * @param targetUserIds 目标用户 ID 集合
     */
    private void send(String title, String content, String linkUrl, Collection<Long> targetUserIds) {
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
        request.setContentType(linkUrl == null ? ContentType.TEXT : ContentType.LINK);
        request.setLinkUrl(linkUrl);
        request.setPriority(NotificationPriority.NORMAL);
        request.setNotificationType(NotificationType.APPROVAL);
        request.setSenderName("系统");
        String notificationId = null;
        try {
            notificationId =
                    notificationTransactionTemplate.execute(
                            txStatus ->
                                    notificationService.createAndPublishToUsers(request, targets));
        } catch (Exception e) {
            log.warn(
                    "BPM 站内通知发送失败（已忽略，不影响审批事务）：title={}，targets={}，原因={}",
                    title,
                    targets,
                    e.getMessage(),
                    e);
        }
        // 企业微信应用消息：站内通知写成功后入发件箱，由调度器异步投递
        if (StringUtils.hasText(notificationId)) {
            try {
                weComMessageOutbox.enqueue(notificationId, targets, title, content, linkUrl);
            } catch (Exception e) {
                // 发件箱入队为可选增强：任何异常都不得影响站内通知与审批事务
                log.warn(
                        "企业微信应用消息入队失败（已忽略，不影响站内通知与审批事务）：notificationId={}，原因={}",
                        notificationId,
                        e.getMessage(),
                        e);
            }
        }
        // 企业微信推送为可选增强：未配置/发送失败均由客户端内部降级，不影响站内通知与审批事务
        weComPushClient.pushAsync(title, content, linkUrl);
    }

    /** 实例结束动作文案；非结束状态返回 null（不发送通知）。 */
    private String finishAction(BpmInstanceStatus status) {
        return switch (status) {
            case APPROVED -> "审批已通过";
            case REJECTED -> "审批已驳回";
            case WITHDRAWN -> "流程已撤回";
            case TERMINATED -> "流程已终止";
            default -> null;
        };
    }

    private String taskLink(Long taskId, Long instanceId) {
        StringBuilder link = new StringBuilder(DEEP_LINK_TODO).append("?tab=pending&taskId=").append(taskId);
        if (instanceId != null) {
            link.append("&instanceId=").append(instanceId);
        }
        return link.toString();
    }

    private String ccLink(BpmInstance instance) {
        Long instanceId = instanceId(instance);
        return instanceId == null
                ? DEEP_LINK_TODO + "?tab=cc"
                : DEEP_LINK_TODO + "?tab=cc&instanceId=" + instanceId;
    }

    private String instanceLink(BpmInstance instance) {
        Long instanceId = instanceId(instance);
        return instanceId == null
                ? DEEP_LINK_TODO + "?tab=initiated"
                : DEEP_LINK_TODO + "?tab=initiated&instanceId=" + instanceId;
    }

    private Long instanceId(BpmInstance instance) {
        return instance == null ? null : instance.getId();
    }

    private String bizType(BpmInstance instance) {
        return instance == null || instance.getBizType() == null ? "-" : instance.getBizType();
    }

    private String bizId(BpmInstance instance) {
        return instance == null || instance.getBizId() == null ? "-" : String.valueOf(instance.getBizId());
    }
}
