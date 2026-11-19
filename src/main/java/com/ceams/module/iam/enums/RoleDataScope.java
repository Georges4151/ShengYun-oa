package com.ceams.module.iam.enums;

import java.util.Locale;

/**
 * 角色数据范围（sys_role.data_scope）。
 *
 * <p>取值 ALL / DEPT / SELF，SELF 为默认值。数据库中的非法值解析为 {@code null}，
 * 由调用方统一回退 SELF。
 */
public enum RoleDataScope {
    /** 仅本人。 */
    SELF,
    /** 本部门及子部门。 */
    DEPT,
    /** 全部。 */
    ALL;

    /** 范围宽度权重（SELF=0，DEPT=1，ALL=2），用于多角色取最宽。 */
    public int weight() {
        return switch (this) {
            case SELF -> 0;
            case DEPT -> 1;
            case ALL -> 2;
        };
    }

    /** 解析字符串（忽略大小写）；空值或非法值返回 null。 */
    public static RoleDataScope from(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
