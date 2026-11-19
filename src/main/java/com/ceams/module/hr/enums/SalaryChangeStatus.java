package com.ceams.module.hr.enums;

/**
 * 薪资变动申请状态（hr_salary_change_apply.status）。
 *
 * <p>该字段只是 {@code bpm_instance.status} 的业务快照，由 BPM 回调写入；
 * 唯一判断依据仍是流程实例状态。
 */
public enum SalaryChangeStatus {
    /** 草稿（当前接口不落库草稿，保留枚举兼容表默认值）。 */
    DRAFT,
    /** 审批中。 */
    APPROVING,
    /** 审批通过。 */
    APPROVED,
    /** 驳回 / 撤回 / 终止（统一收敛为 REJECTED）。 */
    REJECTED
}
