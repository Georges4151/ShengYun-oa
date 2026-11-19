package com.ceams.module.bpm.enums;

/** 任务类型（bpm_task.task_type）。 */
public enum TaskType {
    /** 审批任务。 */
    APPROVE,
    /** 抄送任务（本期抄送走 bpm_cc_record，仅预留类型值）。 */
    CC
}
