package com.ceams.module.expense.enums;

/**
 * 支付方式（biz_receipt.payment_method / biz_receipt_verification.payment_method）。
 *
 * <p>枚举 name 即 varchar 代码值，与契约 PaymentMethod 一致。</p>
 */
public enum PaymentMethod {

    CASH,
    BANK_TRANSFER,
    WECHAT,
    ALIPAY,
    OTHER
}
