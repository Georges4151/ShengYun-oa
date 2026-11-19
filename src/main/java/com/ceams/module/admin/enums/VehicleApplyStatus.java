package com.ceams.module.admin.enums;

import java.util.Locale;

/** 用车申请状态（admin_vehicle_apply.status）。 */
public enum VehicleApplyStatus {
    /** 审批中。 */
    APPROVING,
    /** 审批通过（车辆已置 USING）。 */
    APPROVED,
    /** 已驳回 / 撤回 / 终止。 */
    REJECTED,
    /** 使用中（预留）。 */
    USING,
    /** 已还车。 */
    RETURNED,
    /** 已取消。 */
    CANCELLED;

    /** 编码解析，未知返回 null。 */
    public static VehicleApplyStatus from(String code) {
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
