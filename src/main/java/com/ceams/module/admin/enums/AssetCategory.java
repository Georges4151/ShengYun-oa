package com.ceams.module.admin.enums;

import java.util.Locale;

/** 资产分类（admin_asset.category）。 */
public enum AssetCategory {
    /** 电子设备。 */
    ELECTRONIC,
    /** 家具。 */
    FURNITURE,
    /** 车辆 / 工具。 */
    VEHICLE_TOOL,
    /** 其他。 */
    OTHER;

    /** 编码解析，未知返回 null。 */
    public static AssetCategory from(String code) {
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
