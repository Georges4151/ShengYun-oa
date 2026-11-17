package com.ceams.module.admin.enums;

import java.util.Locale;

/** 报修工单状态（admin_repair.status）。 */
public enum RepairStatus {
    /** 已提交（审批中）。 */
    SUBMITTED,
    /** 已受理（审批通过，待派单）。 */
    ASSIGNED,
    /** 处理中。 */
    PROCESSING,
    /** 已完工（待验收）。 */
    DONE,
    /** 已验收。 */
    ACCEPTED,
    /** 已驳回（仅由审批回调写入）。 */
    REJECTED;

    /** 编码解析，未知返回 null。 */
    public static RepairStatus from(String code) {
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
