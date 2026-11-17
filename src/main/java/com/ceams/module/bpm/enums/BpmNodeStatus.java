package com.ceams.module.bpm.enums;

/** 节点实例状态（bpm_node.status）。 */
public enum BpmNodeStatus {
    /** 未到达。 */
    PENDING,
    /** 已到达，等待处理。 */
    ACTIVE,
    /** 已完成。 */
    COMPLETED,
    /** 被驳回。 */
    REJECTED,
    /** 已关闭（撤回/终止/跳转覆盖）。 */
    CLOSED
}
