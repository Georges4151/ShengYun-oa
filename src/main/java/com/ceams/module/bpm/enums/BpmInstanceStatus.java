package com.ceams.module.bpm.enums;

/** 流程实例状态（bpm_instance.status）。 */
public enum BpmInstanceStatus {
    /** 草稿（预留：当前发起即 RUNNING）。 */
    DRAFT,
    /** 运行中。 */
    RUNNING,
    /** 审批通过。 */
    APPROVED,
    /** 驳回终止。 */
    REJECTED,
    /** 发起人撤回。 */
    WITHDRAWN,
    /** 管理员终止。 */
    TERMINATED
}
