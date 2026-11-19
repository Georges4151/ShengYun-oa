package com.ceams.module.doc.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ceams.module.doc.config.DocIndexConfiguration;
import com.ceams.module.doc.config.DocProperties;
import com.ceams.module.doc.entity.DocChunk;
import com.ceams.module.doc.entity.DocFile;
import com.ceams.module.doc.entity.DocFileText;
import com.ceams.module.doc.entity.DocIndexTask;
import com.ceams.module.doc.enums.DocIndexStatus;
import com.ceams.module.doc.mapper.DocChunkMapper;
import com.ceams.module.doc.mapper.DocFileMapper;
import com.ceams.module.doc.mapper.DocFileTextMapper;
import com.ceams.module.doc.mapper.DocIndexTaskMapper;
import com.ceams.module.doc.service.DocIndexService;
import com.ceams.module.doc.support.DocChunker;
import com.ceams.module.doc.support.DocStorageService;
import com.ceams.module.doc.support.DocTextExtractor;
import java.util.List;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 文档索引服务实现（抽取 → 分块 → 写入检索语料）。
 *
 * <p>异步与事务安全约定：
 *
 * <ul>
 *   <li>索引线程只依赖 {@code fileId / taskId}，<b>禁止</b>使用 {@code SecurityUtils} 等登录态 API；
 *   <li>任何异常都收敛为任务 FAILED（error_msg 截断 1000），不向线程池抛出；
 *   <li>任务落库与任务执行各用 REQUIRES_NEW 事务：避免在 {@code afterCommit} 阶段复用已提交的
 *       连接导致写入丢失，同时保证单次索引的语料写入原子生效 / 回滚；
 *   <li>任务先落库再入队，便于失败追溯；线程池拒绝时把任务置 FAILED，不影响上传主链路。
 * </ul>
 */
@Slf4j
@Service
public class DocIndexServiceImpl implements DocIndexService {

    /** 任务类型：抽取 + 分块。 */
    private static final String TASK_TYPE_EXTRACT = "EXTRACT";

    /** error_msg 列长度上限。 */
    private static final int MAX_ERROR_LENGTH = 1000;

    /** 分块长度兜底值（配置非法时使用）。 */
    private static final int DEFAULT_CHUNK_SIZE = 600;

    /** 分块重叠兜底值（配置非法时使用）。 */
    private static final int DEFAULT_CHUNK_OVERLAP = 100;

    private final DocIndexTaskMapper taskMapper;
    private final DocFileTextMapper fileTextMapper;
    private final DocChunkMapper chunkMapper;
    private final DocFileMapper fileMapper;
    private final DocStorageService storageService;
    private final DocTextExtractor textExtractor;
    private final DocProperties docProperties;
    private final ThreadPoolTaskExecutor docIndexTaskExecutor;

    /** REQUIRES_NEW 事务模板：任务落库与单次索引各自独立提交，不受调用方事务影响。 */
    private final TransactionTemplate requiresNewTransaction;

    public DocIndexServiceImpl(
            DocIndexTaskMapper taskMapper,
            DocFileTextMapper fileTextMapper,
            DocChunkMapper chunkMapper,
            DocFileMapper fileMapper,
            DocStorageService storageService,
            DocTextExtractor textExtractor,
            DocProperties docProperties,
            @Qualifier(DocIndexConfiguration.EXECUTOR_BEAN_NAME)
                    ThreadPoolTaskExecutor docIndexTaskExecutor,
            PlatformTransactionManager transactionManager) {
        this.taskMapper = taskMapper;
        this.fileTextMapper = fileTextMapper;
        this.chunkMapper = chunkMapper;
        this.fileMapper = fileMapper;
        this.storageService = storageService;
        this.textExtractor = textExtractor;
        this.docProperties = docProperties;
        this.docIndexTaskExecutor = docIndexTaskExecutor;
        this.requiresNewTransaction = new TransactionTemplate(transactionManager);
        this.requiresNewTransaction.setPropagationBehavior(
                TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Override
    public void submit(Long fileId, int versionNo) {
        if (fileId == null) {
            return;
        }
        Long taskId;
        try {
            // 独立事务提交任务行，确保事务同步的 afterCommit 阶段不会复用已提交连接
            taskId =
                    requiresNewTransaction.execute(
                            status -> {
                                DocIndexTask task = new DocIndexTask();
                                task.setFileId(fileId);
                                task.setTaskType(TASK_TYPE_EXTRACT);
                                task.setVersionNo(versionNo);
                                task.setStatus(DocIndexStatus.PENDING);
                                task.setRetryCount(0);
                                taskMapper.insert(task);
                                return task.getId();
                            });
        } catch (RuntimeException e) {
            log.warn("索引任务落库失败，跳过本次索引：fileId={}，versionNo={}", fileId, versionNo, e);
            return;
        }
        try {
            docIndexTaskExecutor.execute(() -> process(taskId));
        } catch (TaskRejectedException e) {
            log.warn("索引线程池已满，任务置为 FAILED：taskId={}，fileId={}", taskId, fileId);
            markFailed(taskId, "索引线程池已满，请稍后重试");
        }
    }

    @Override
    public void submitAfterCommit(Long fileId, int versionNo) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            submit(fileId, versionNo);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        submit(fileId, versionNo);
                    }
                });
    }

    @Override
    public void reindexAll() {
        List<DocFile> files =
                fileMapper.selectList(
                        new LambdaQueryWrapper<DocFile>()
                                .select(DocFile::getId, DocFile::getCurrentVersionNo));
        if (files == null || files.isEmpty()) {
            log.info("全量重建索引：没有可索引的文档");
            return;
        }
        for (DocFile file : files) {
            if (file == null || file.getId() == null) {
                continue;
            }
            int versionNo = file.getCurrentVersionNo() == null ? 1 : file.getCurrentVersionNo();
            submit(file.getId(), versionNo);
        }
        log.info("全量重建索引已提交：共 {} 个文档", files.size());
    }

    /**
     * 执行单个索引任务（异步线程内调用，不依赖登录态）。
     *
     * <p>任务体在 REQUIRES_NEW 事务内执行：中途异常回滚语料写入，再由外层把任务置 FAILED。
     *
     * @param taskId 任务 ID（为空直接忽略）
     */
    void process(Long taskId) {
        if (taskId == null) {
            return;
        }
        try {
            requiresNewTransaction.executeWithoutResult(status -> processInTransaction(taskId));
        } catch (Exception e) {
            log.warn("文档索引任务执行失败：taskId={}", taskId, e);
            markFailed(taskId, safeMessage(e));
        }
    }

    /** 索引任务主体（在独立事务内执行，状态更新随事务提交）。 */
    private void processInTransaction(Long taskId) {
        DocIndexTask task = taskMapper.selectById(taskId);
        if (task == null) {
            log.warn("索引任务不存在，忽略：taskId={}", taskId);
            return;
        }
        task.setStatus(DocIndexStatus.RUNNING);
        taskMapper.updateById(task);

        DocFile file = task.getFileId() == null ? null : fileMapper.selectById(task.getFileId());
        if (file == null) {
            markSkipped(task, "文件不存在或已删除");
            return;
        }
        if (!ragEnabled()) {
            markSkipped(task, "RAG 未启用");
            return;
        }
        String ext = resolveExt(file);
        if (!DocTextExtractor.supports(ext)) {
            String reason = StringUtils.isBlank(ext) ? "无扩展名" : ext;
            markSkipped(task, "不支持的文件类型：" + reason);
            return;
        }
        if (isUpToDate(file)) {
            markSuccess(task);
            return;
        }

        String text = textExtractor.extract(storageService.resolve(file.getStorageKey()), ext);
        if (StringUtils.isBlank(text)) {
            markSkipped(task, "未抽取到有效文本");
            return;
        }

        saveFileText(file, text);
        int chunkCount = rebuildChunks(file, text);
        markSuccess(task);
        log.info(
                "文档索引完成：fileId={}，versionNo={}，chars={}，chunks={}",
                file.getId(),
                task.getVersionNo(),
                text.length(),
                chunkCount);
    }

    // ------------------------------------------------------------------
    // 内部实现
    // ------------------------------------------------------------------

    /** 语料是否已是最新：抽取文本 checksum 与文件一致，且仍有未删除分块。 */
    private boolean isUpToDate(DocFile file) {
        if (StringUtils.isBlank(file.getChecksum())) {
            return false;
        }
        DocFileText existing = findFileText(file.getId());
        if (existing == null
                || !StringUtils.equalsIgnoreCase(existing.getChecksum(), file.getChecksum())) {
            return false;
        }
        Long chunks =
                chunkMapper.selectCount(
                        new LambdaQueryWrapper<DocChunk>().eq(DocChunk::getFileId, file.getId()));
        return chunks != null && chunks > 0;
    }

    /** 按文件 ID 查询抽取文本（逻辑删除行不参与）。 */
    private DocFileText findFileText(Long fileId) {
        return fileTextMapper.selectOne(
                new LambdaQueryWrapper<DocFileText>()
                        .eq(DocFileText::getFileId, fileId)
                        .last("LIMIT 1"));
    }

    /** 写入 / 更新抽取文本（唯一键 file_id，冲突时转为更新）。 */
    private void saveFileText(DocFile file, String text) {
        int versionNo = file.getCurrentVersionNo() == null ? 1 : file.getCurrentVersionNo();
        DocFileText existing = findFileText(file.getId());
        if (existing == null) {
            DocFileText entity = new DocFileText();
            entity.setFileId(file.getId());
            entity.setVersionNo(versionNo);
            entity.setChecksum(file.getChecksum());
            entity.setContent(text);
            entity.setCharCount(text.length());
            entity.setExtractStatus(DocFileText.STATUS_READY);
            entity.setErrorMsg(null);
            try {
                fileTextMapper.insert(entity);
                return;
            } catch (DuplicateKeyException e) {
                // 并发索引同一文件：唯一键冲突后走更新
                log.debug("抽取文本已存在，转为更新：fileId={}", file.getId());
            }
        }
        int updated =
                fileTextMapper.update(
                        null,
                        new LambdaUpdateWrapper<DocFileText>()
                                .eq(DocFileText::getFileId, file.getId())
                                .set(DocFileText::getVersionNo, versionNo)
                                .set(DocFileText::getChecksum, file.getChecksum())
                                .set(DocFileText::getContent, text)
                                .set(DocFileText::getCharCount, text.length())
                                .set(DocFileText::getExtractStatus, DocFileText.STATUS_READY)
                                .set(DocFileText::getErrorMsg, null));
        if (updated == 0) {
            log.warn("抽取文本既无法插入也无法更新（可能被逻辑删除）：fileId={}", file.getId());
        }
    }

    /** 逻辑删除旧分块后按序重建，返回新分块数。 */
    private int rebuildChunks(DocFile file, String text) {
        chunkMapper.delete(
                new LambdaQueryWrapper<DocChunk>().eq(DocChunk::getFileId, file.getId()));
        List<String> pieces = DocChunker.split(text, chunkSize(), chunkOverlap());
        int chunkNo = 0;
        for (String piece : pieces) {
            DocChunk chunk = new DocChunk();
            chunk.setFileId(file.getId());
            chunk.setChunkNo(chunkNo++);
            chunk.setContent(piece);
            chunk.setCharCount(piece.length());
            // M2 以字符数近似 token 数；page_no / embedding_id 预留给 M3
            chunk.setTokenCount(piece.length());
            chunkMapper.insert(chunk);
        }
        return pieces.size();
    }

    /** 任务置 SUCCESS。 */
    private void markSuccess(DocIndexTask task) {
        task.setStatus(DocIndexStatus.SUCCESS);
        task.setErrorMsg(null);
        updateQuietly(task);
    }

    /** 任务置 SKIPPED 并记录原因。 */
    private void markSkipped(DocIndexTask task, String reason) {
        task.setStatus(DocIndexStatus.SKIPPED);
        task.setErrorMsg(truncate(reason));
        updateQuietly(task);
        log.info("文档索引跳过：fileId={}，原因={}", task.getFileId(), reason);
    }

    /** 任务置 FAILED（按 ID 更新，异常时不再抛出）。 */
    private void markFailed(Long taskId, String reason) {
        if (taskId == null) {
            return;
        }
        DocIndexTask update = new DocIndexTask();
        update.setId(taskId);
        update.setStatus(DocIndexStatus.FAILED);
        update.setErrorMsg(truncate(reason));
        updateQuietly(update);
    }

    /** 状态更新失败只记日志，不影响主流程。 */
    private void updateQuietly(DocIndexTask task) {
        try {
            taskMapper.updateById(task);
        } catch (RuntimeException e) {
            log.warn("索引任务状态更新失败：taskId={}，status={}", task.getId(), task.getStatus(), e);
        }
    }

    /** RAG 开关（配置缺失时按开启处理）。 */
    private boolean ragEnabled() {
        return docProperties.getRag() == null || docProperties.getRag().isEnabled();
    }

    /** 分块长度（非法配置回退默认值）。 */
    private int chunkSize() {
        int configured = docProperties.getRag() == null ? 0 : docProperties.getRag().getChunkSize();
        return configured > 0 ? configured : DEFAULT_CHUNK_SIZE;
    }

    /** 分块重叠（负数按 0，超大在 chunker 内再收敛）。 */
    private int chunkOverlap() {
        int configured =
                docProperties.getRag() == null ? 0 : docProperties.getRag().getChunkOverlap();
        return configured >= 0 ? configured : DEFAULT_CHUNK_OVERLAP;
    }

    /** 扩展名：优先实体 ext，缺失时从文件名推导。 */
    private String resolveExt(DocFile file) {
        if (StringUtils.isNotBlank(file.getExt())) {
            return file.getExt().trim().toLowerCase(Locale.ROOT);
        }
        String name = file.getName();
        if (StringUtils.isBlank(name)) {
            return "";
        }
        int dot = name.lastIndexOf('.');
        return dot < 0 || dot == name.length() - 1
                ? ""
                : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    /** error_msg 截断到列长度。 */
    private String truncate(String message) {
        if (message == null) {
            return null;
        }
        return message.length() <= MAX_ERROR_LENGTH
                ? message
                : message.substring(0, MAX_ERROR_LENGTH);
    }

    private String safeMessage(Exception e) {
        String message = e.getMessage();
        return StringUtils.isNotBlank(message) ? message : e.getClass().getSimpleName();
    }
}
