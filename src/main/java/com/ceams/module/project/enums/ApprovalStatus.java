package com.ceams.module.project.enums;

/**
 * 审批状态（biz_change_request.status / biz_progress_payment.status）。
 *
 * <p>枚举 name 即 varchar 代码值，与契约 ApprovalStatus 一致。</p>
 */
public enum ApprovalStatus {

    DRAFT,
    PENDING,
    APPROVED,
    REJECTED
}