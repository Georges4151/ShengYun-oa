package com.ceams.module.bpm.enums;

/** 审批动作（用于幂等判定与审计动作映射）。 */
public enum BpmTaskAction {
    /** 同意。 */
    APPROVE,
    /** 驳回。 */
    REJECT,
    /** 改派。 */
    DELEGATE,
    /** 撤回（实例级动作）。 */
    WITHDRAW,
    /** 终止（实例级动作）。 */
    TERMINATE
}
