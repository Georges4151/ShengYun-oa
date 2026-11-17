package com.ceams.module.doc.enums;

import java.util.Locale;

/** 文档版本来源（doc_file_version.source）。 */
public enum DocVersionSource {
    /** 新上传。 */
    UPLOAD,
    /** 在线编辑保存。 */
    ONLINE_EDIT,
    /** 版本回滚。 */
    ROLLBACK;

    /** 编码解析，未知返回 null（调用方按参数非法处理）。 */
    public static DocVersionSource from(String code) {
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
