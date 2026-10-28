package com.ceams.module.expense.enums;

/**
 * 财务核验状态（biz_receipt.verification_status）。
 *
 * <p>枚举 name 即 varchar 代码值，与契约 VerificationStatus 一致。</p>
 */
public enum VerificationStatus {

    PENDING,
    VERIFIED,
    REJECTED
}
