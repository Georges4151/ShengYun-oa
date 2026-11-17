package com.ceams.module.doc.enums;

import java.util.Locale;

/** 文档库类型（doc_directory.library_type）。 */
public enum LibraryType {
    /** 个人库：仅 owner_user_id 本人可见。 */
    PERSONAL,
    /** 部门库：owner_dept_id 本部门及子部门可见。 */
    DEPT,
    /** 项目库：项目经理与显式授权可见。 */
    PROJECT,
    /** 公共库：所有登录用户可见。 */
    PUBLIC;

    /** 编码解析，未知返回 null（调用方按参数非法处理）。 */
    public static LibraryType from(String code) {
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
