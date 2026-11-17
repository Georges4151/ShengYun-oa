package com.ceams.module.doc.enums;

import java.util.Locale;

/** 文档文件状态（doc_file.status）。 */
public enum DocFileStatus {
    /** 正常。 */
    NORMAL,
    /** 编辑中（仅用于列表展示，真实锁以 Redis 为准）。 */
    LOCKED;

    /** 编码解析，未知返回 null（调用方按参数非法处理）。 */
    public static DocFileStatus from(String code) {
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
