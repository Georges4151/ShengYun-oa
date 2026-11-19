package com.ceams.module.integration.enums;

/** 企业微信应用消息状态（integration_wecom_message.status）。 */
public enum WeComMessageStatus {
    /** 待投递（含等待下次重试）。 */
    PENDING,
    /** 投递成功。 */
    SENT,
    /** 超过最大重试次数，投递失败（可手工重投）。 */
    FAILED
}
