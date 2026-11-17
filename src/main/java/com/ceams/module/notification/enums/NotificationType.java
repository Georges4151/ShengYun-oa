package com.ceams.module.notification.enums;

/** 通知类型（代码值，可按业务扩展）。 */
public enum NotificationType {

    /** 系统通知。 */
    SYSTEM,

    /** 审批待办通知。 */
    APPROVAL,

    /** 公告通知。 */
    ANNOUNCEMENT,

    /** 人力资源通知（工资条发布、劳动合同到期）。 */
    HR,

    /** 行政通知（资产借用逾期、报修派单）。 */
    ADMIN,

    /** 文档通知（共享 / 变更，本期仅预留枚举与筛选）。 */
    DOC
}
