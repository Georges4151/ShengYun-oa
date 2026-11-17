package com.ceams.module.admin.enums;

import java.util.Locale;

/** 易耗品状态（admin_material.status）。 */
public enum MaterialStatus {
    /** 启用（可申领）。 */
    ENABLED,
    /** 停用。 */
    DISABLED;

    /** 编码解析，未知返回 null。 */
    public static MaterialStatus from(String code) {
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
