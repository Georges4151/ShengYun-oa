package com.ceams.module.doc.enums;

import java.util.Locale;

/** 授权主体类型（doc_dir_permission.subject_type / doc_file_permission.subject_type）。 */
public enum DocSubjectType {
    /** 用户（subject_id = sys_user.id）。 */
    USER,
    /** 角色（subject_id = sys_role.id）。 */
    ROLE,
    /** 部门（subject_id = sys_dept.id）。 */
    DEPT;

    /** 编码解析，未知返回 null（调用方按参数非法处理）。 */
    public static DocSubjectType from(String code) {
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
