package com.ceams.module.admin.enums;

import java.util.Locale;

/** 报修分类（admin_repair.category）。 */
public enum RepairCategory {
    /** 电气。 */
    ELECTRIC,
    /** 给排水。 */
    WATER,
    /** 家具。 */
    FURNITURE,
    /** 信息技术。 */
    IT,
    /** 其他。 */
    OTHER;

    /** 编码解析，未知返回 null。 */
    public static RepairCategory from(String code) {
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
