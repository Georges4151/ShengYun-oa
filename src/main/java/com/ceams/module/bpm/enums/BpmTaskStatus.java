package com.ceams.module.bpm.enums;

/** 任务状态（bpm_task.status）。 */
public enum BpmTaskStatus {
    /** 待处理。 */
    PENDING,
    /** 待分配：审批人解析为空，不进入个人待办。 */
    PENDING_ASSIGNEE,
    /** 已同意。 */
    APPROVED,
    /** 已驳回。 */
    REJECTED,
    /** 已关闭（撤回/终止/或签他人同意/改派旧任务）。 */
    CLOSED
}
