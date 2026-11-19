package com.ceams.module.doc.enums;

import java.util.Locale;

/** 文档索引任务状态（doc_index_task.status）。 */
public enum DocIndexStatus {
    /** 待执行。 */
    PENDING,
    /** 执行中。 */
    RUNNING,
    /** 索引成功。 */
    SUCCESS,
    /** 索引失败（error_msg 记录原因，可人工重跑）。 */
    FAILED,
    /** 已跳过（未启用 / 类型不支持 / 无可抽取文本 / 文件已删除）。 */
    SKIPPED;

    /** 编码解析，未知返回 null（调用方按参数非法处理）。 */
    public static DocIndexStatus from(String code) {
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
