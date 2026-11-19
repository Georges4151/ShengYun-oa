package com.ceams.module.doc.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ceams.common.base.BaseEntity;
import com.ceams.module.doc.enums.DocIndexStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 文档索引任务（doc_index_task，异步抽取与分块的执行流水）。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("doc_index_task")
public class DocIndexTask extends BaseEntity {

    /** 文件 ID（逻辑引用 doc_file.id）。 */
    private Long fileId;

    /** 任务类型：EXTRACT（抽取 + 分块）。 */
    private String taskType;

    /** 触发索引时的文件版本号。 */
    private Integer versionNo;

    /** 任务状态：PENDING/RUNNING/SUCCESS/FAILED/SKIPPED。 */
    private DocIndexStatus status;

    /** 重试次数（人工重跑累加）。 */
    private Integer retryCount;

    /** 失败原因（截断 1000 字符）。 */
    private String errorMsg;
}
