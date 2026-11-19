package com.ceams.module.admin.enums;

import java.util.Locale;

/** 资产状态（admin_asset.status）。 */
public enum AssetStatus {
    /** 在库。 */
    IN_STOCK,
    /** 已借出。 */
    BORROWED,
    /** 维修中。 */
    REPAIRING,
    /** 已报废（不可逆）。 */
    SCRAPPED;

    /** 编码解析，未知返回 null（调用方按参数非法处理）。 */
    public static AssetStatus from(String code) {
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
