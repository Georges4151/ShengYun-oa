package com.ceams.module.doc.service;

/**
 * 文档索引服务（抽取文本 → 分块 → 写入检索语料）。
 *
 * <p>索引一律异步执行，且只依赖 fileId / versionNo，不依赖登录态；业务侧在写库成功后触发即可，
 * 索引失败不会影响上传、回滚或在线保存主链路。
 */
public interface DocIndexService {

    /**
     * 提交索引任务：先落 doc_index_task(PENDING)，再异步执行。
     *
     * @param fileId 文件 ID
     * @param versionNo 触发索引的版本号（用于追溯，不强制等于当前版本）
     */
    void submit(Long fileId, int versionNo);

    /**
     * 事务内提交索引任务：当前事务提交成功后再入队，避免索引线程读到未提交数据。
     *
     * <p>无活动事务时等价于 {@link #submit(Long, int)}。
     *
     * @param fileId 文件 ID
     * @param versionNo 触发索引的版本号
     */
    void submitAfterCommit(Long fileId, int versionNo);

    /** 全量重建（存量文档与运维使用）：对全部未删除文档逐个提交索引任务。 */
    void reindexAll();
}
