package com.ceams.module.admin.enums;

import java.util.Locale;

/** 访客状态（admin_visitor.status）。 */
public enum VisitorStatus {
    /** 待核销。 */
    PENDING,
    /** 已入场。 */
    CHECKED_IN,
    /** 已离场。 */
    CHECKED_OUT,
    /** 已取消。 */
    CANCELLED;

    /** 编码解析，未知返回 null。 */
    public static VisitorStatus from(String code) {
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
