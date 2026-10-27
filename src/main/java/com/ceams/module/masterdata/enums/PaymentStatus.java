package com.ceams.module.masterdata.enums;

/**
 * 付款状态（biz_equipment_lease.payment_status）。
 *
 * <p>枚举 name 即 varchar 代码值，与契约 EquipmentLease 的 paymentStatus 一致。</p>
 */
public enum PaymentStatus {

    UNPAID,
    PARTIAL,
    PAID
}