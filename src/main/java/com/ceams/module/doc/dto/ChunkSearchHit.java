package com.ceams.module.doc.dto;

import lombok.Data;

/** 分块检索命中（doc_chunk 候选召回结果，仅内部使用，不含任何授权信息）。 */
@Data
public class ChunkSearchHit {

    /** 分块主键。 */
    private Long id;

    /** 文件 ID（用于反查 doc_file / doc_directory 做权限过滤）。 */
    private Long fileId;

    /** 分块序号（同一文件内从 0 递增）。 */
    private Integer chunkNo;

    /** 分块文本。 */
    private String content;

    /** 相关度：全文检索为 MATCH 得分，LIKE 兜底固定 0。 */
    private double score;
}
