package com.ceams.module.doc.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ceams.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 文档抽取文本（doc_file_text，唯一键 file_id，供 RAG 语料复用）。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("doc_file_text")
public class DocFileText extends BaseEntity {

    /** 抽取状态：已就绪。 */
    public static final String STATUS_READY = "READY";

    /** 抽取状态：已跳过（不支持的类型 / 无可抽取文本）。 */
    public static final String STATUS_SKIPPED = "SKIPPED";

    /** 文件 ID（逻辑引用 doc_file.id）。 */
    private Long fileId;

    /** 抽取时的文件版本号（从 1 起）。 */
    private Integer versionNo;

    /** 抽取时文件的 SHA-256（与 doc_file.checksum 比对判定是否重建）。 */
    private String checksum;

    /** 抽取后的纯文本（已归一化并按上限截断）。 */
    private String content;

    /** 文本字符数。 */
    private Integer charCount;

    /** 抽取状态：READY/SKIPPED。 */
    private String extractStatus;

    /** 抽取失败原因（截断 1000 字符，成功为空）。 */
    private String errorMsg;
}
