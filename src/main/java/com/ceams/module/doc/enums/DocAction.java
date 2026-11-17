package com.ceams.module.doc.enums;

import java.util.Locale;

/** 文档动作（doc_dir_permission.actions / doc_file_permission.actions 的 JSON 数组取值）。 */
public enum DocAction {
    /** 预览。 */
    PREVIEW,
    /** 下载。 */
    DOWNLOAD,
    /** 编辑（含重命名、移动、在线编辑）。 */
    EDIT,
    /** 删除。 */
    DELETE;

    /** 编码解析，未知返回 null（调用方按参数非法处理）。 */
    public static DocAction from(String code) {
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
