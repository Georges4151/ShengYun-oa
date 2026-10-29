package com.ceams.module.expense.enums;

/**
 * 票据审批状态（biz_receipt.approval_status，V5 补充列）。
 *
 * <p>枚举 name 即 varchar 代码值，与契约 ReceiptApprovalStatus 一致。
 */
public enum ReceiptApprovalStatus {
    DRAFT,
    PENDING,
    APPROVED,
    REJECTED
}
