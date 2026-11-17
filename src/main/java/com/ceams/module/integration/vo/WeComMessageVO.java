package com.ceams.module.integration.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.time.LocalDateTime;
import lombok.Data;

/** 企业微信应用消息（发件箱）展示对象。 */
@Data
public class WeComMessageVO {

    /** 消息主键（序列化为字符串，避免前端精度丢失）。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 站内通知 ID（序列化为字符串，避免前端精度丢失）。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long notificationId;

    /** 目标用户 ID（序列化为字符串，避免前端精度丢失）。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long targetUserId;

    /** 企业微信成员账号（入队时快照）。 */
    private String wecomUserid;

    /** 消息类型：当前仅 TEXT。 */
    private String msgType;

    /** 状态：PENDING/SENT/FAILED。 */
    private String status;

    /** 已重试次数。 */
    private Integer retryCount;

    /** 下次重试时间。 */
    private LocalDateTime nextRetryAt;

    /** 发送成功时间。 */
    private LocalDateTime sentAt;

    /** 最近一次失败原因。 */
    private String errorMsg;

    /** 创建时间。 */
    private LocalDateTime createTime;
}
