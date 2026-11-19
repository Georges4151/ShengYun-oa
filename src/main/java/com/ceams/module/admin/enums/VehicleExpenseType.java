package com.ceams.module.admin.enums;

import java.util.Locale;

/** 车辆费用类型（admin_vehicle_expense.expense_type）。 */
public enum VehicleExpenseType {
    /** 维修保养。 */
    MAINTENANCE,
    /** 违章罚款。 */
    VIOLATION,
    /** 加油。 */
    FUEL;

    /** 编码解析，未知返回 null。 */
    public static VehicleExpenseType from(String code) {
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
