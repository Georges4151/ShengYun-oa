package com.ceams.module.admin.enums;

import java.util.Locale;

/** 易耗品申领单状态（admin_material_apply.status）。 */
public enum MaterialApplyStatus {
    /** 草稿。 */
    DRAFT,
    /** 审批中。 */
    APPROVING,
    /** 审批通过（库存已扣减，待发放）。 */
    APPROVED,
    /** 已驳回 / 撤回 / 终止。 */
    REJECTED,
    /** 已发放。 */
    ISSUED;

    /** 编码解析，未知返回 null。 */
    public static MaterialApplyStatus from(String code) {
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
