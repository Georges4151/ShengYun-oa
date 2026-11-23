package com.ceams.common.enums;

/**
 * 合同审批状态快照（hr_contract / biz_subcontractor_contract 的 approval_status）。
 *
 * <p>唯一判断依据是 {@code bpm_instance.status}；合同表上的该字段仅由 BPM 回调写入，
 * 默认 NONE 表示尚未提交审批。
 */
public enum ContractApprovalStatus {
    /** 未提交。 */
    NONE,
    /** 审批中。 */
    PENDING,
    /** 审批通过。 */
    APPROVED,
    /** 已驳回（含撤回、终止）。 */
    REJECTED
}
