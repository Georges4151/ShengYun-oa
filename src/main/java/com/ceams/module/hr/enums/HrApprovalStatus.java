package com.ceams.module.hr.enums;

/**
 * HR 申请单审批状态（hr_employee_change / hr_leave_request / hr_checkin_supplement）。
 *
 * <p>该字段只是 {@code bpm_instance.status} 的业务快照，由 BPM 回调写入；
 * 唯一判断依据仍是流程实例状态。
 */
public enum HrApprovalStatus {
    /** 审批中。 */
    PENDING,
    /** 审批通过。 */
    APPROVED,
    /** 驳回。 */
    REJECTED,
    /** 发起人撤回。 */
    WITHDRAWN,
    /** 管理员终止（业务统一口径，历史数据可能为 TERMINATED）。 */
    CANCELLED,
    /** 管理员终止（仅历史数据兼容，新数据不再写入）。 */
    TERMINATED
}
