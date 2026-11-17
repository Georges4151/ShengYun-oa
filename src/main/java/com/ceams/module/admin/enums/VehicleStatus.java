package com.ceams.module.admin.enums;

import java.util.Locale;

/** 车辆状态（admin_vehicle.status）。 */
public enum VehicleStatus {
    /** 空闲。 */
    IDLE,
    /** 使用中。 */
    USING,
    /** 维修保养。 */
    MAINTENANCE;

    /** 编码解析，未知返回 null。 */
    public static VehicleStatus from(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        try {
            return valueOf(code.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
