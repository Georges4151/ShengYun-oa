package com.ceams.module.integration.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ceams.common.base.BaseEntity;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 企业微信应用消息发件箱（integration_wecom_message）。
 *
 * <p>P2 仅建立实体与 Mapper，实际入队与投递由 P4 实现。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("integration_wecom_message")
public class IntegrationWecomMessage extends BaseEntity {

    /** 站内通知 ID（逻辑引用 biz_notification.id）。 */
    private Long notificationId;

    /** 目标用户 ID（逻辑引用 sys_user.id）。 */
    private Long targetUserId;

    /** 企业微信成员账号（入队时快照）。 */
    private String wecomUserid;

    /** 消息类型，当前仅 TEXT。 */
    private String msgType = "TEXT";

    /** 消息体快照（title/content/linkUrl）。 */
    private String payloadJson;

    /** 状态：PENDING/SENT/FAILED。 */
    private String status = "PENDING";

    /** 已重试次数。 */
    private Integer retryCount = 0;

    /** 下次重试时间。 */
    private LocalDateTime nextRetryAt;

    /** 最近一次失败原因。 */
    private String errorMsg;

    /** 发送成功时间。 */
    private LocalDateTime sentAt;
}
