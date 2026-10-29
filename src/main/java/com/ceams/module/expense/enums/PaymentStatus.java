package com.ceams.module.expense.enums;

/**
 * 付款状态（biz_receipt.payment_status）。
 *
 * <p>枚举 name 即 varchar 代码值，与契约 PaymentStatus 一致。
 */
public enum PaymentStatus {
    UNPAID,
    PARTIAL,
    PAID
}
