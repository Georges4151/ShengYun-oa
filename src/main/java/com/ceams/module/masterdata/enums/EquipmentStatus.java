package com.ceams.module.masterdata.enums;

/**
 * 设备状态（biz_equipment.status）。
 *
 * <p>枚举 name 即 varchar 代码值，与契约 EquipmentStatus 一致。
 */
public enum EquipmentStatus {
    IDLE,
    IN_USE,
    LEASED,
    MAINTENANCE,
    SCRAPPED
}
