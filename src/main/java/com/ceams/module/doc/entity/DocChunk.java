package com.ceams.module.doc.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ceams.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 文档分块语料（doc_chunk，同一文件 chunk_no 从 0 递增，用于全文检索）。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("doc_chunk")
public class DocChunk extends BaseEntity {

    /** 文件 ID（逻辑引用 doc_file.id）。 */
    private Long fileId;

    /** 分块序号（同一文件内从 0 递增）。 */
    private Integer chunkNo;

    /** 分块文本。 */
    private String content;

    /** 分块字符数。 */
    private Integer charCount;

    /** 页码（M2 暂为空，预留给 M3 引用定位）。 */
    private Integer pageNo;

    /** 分词数（M2 以字符数近似，M3 向量化可替换）。 */
    private Integer tokenCount;

    /** 向量 ID（M3 预留，M2 不写入）。 */
    private String embeddingId;
}
