-- ======================================================================
-- V44：文档知识库 RAG - 抽取文本 / 分块语料 / 索引任务
--
-- 用途：
--   1. doc_file_text：按文件维度保存 Tika 抽取并归一化后的纯文本，checksum 与
--      doc_file.checksum 比对即可判断语料是否需要重建（同一内容不重复抽取）。
--   2. doc_chunk：把纯文本切分为定长片段（默认 600 字、重叠 100 字），
--      content 上建 ngram 全文索引支撑中文检索。
--   3. doc_index_task：异步索引任务流水（PENDING/RUNNING/SUCCESS/FAILED/SKIPPED），
--      便于失败重试与运维排查。
--
-- 权限口径（先收窄再召回）：
--   全文检索只能作为「候选召回」，不是授权依据。检索结果必须先按
--   doc_file / doc_directory 反查，经 DocAccessResolver 判定含 PREVIEW 动作后
--   才能返回给调用方；本脚本的三张表均不含任何授权字段，禁止用于越权判断。
--
-- 约定：
--   1. 主键为雪花 ID（BIGINT），不 AUTO_INCREMENT。
--   2. 本脚本为增量迁移，入库后不得修改；如需变更另起更高版本号脚本。
--   3. 业务侧禁止把 storage_key / checksum / 物理路径返回给模型或前端。
-- ======================================================================

-- ----------------------------------------------------------------------
-- 1. 文档抽取文本（doc_file_text，一个文件一条）
-- ----------------------------------------------------------------------
CREATE TABLE doc_file_text (
    id             BIGINT       NOT NULL                COMMENT '主键（雪花ID）',
    file_id        BIGINT       NOT NULL                COMMENT '文件ID（逻辑引用 doc_file.id）',
    version_no     INT          NOT NULL                COMMENT '抽取时的文件版本号（从 1 起）',
    checksum       CHAR(64)     NOT NULL                COMMENT '抽取时文件的 SHA-256（与 doc_file.checksum 比对判定是否重建）',
    content        MEDIUMTEXT   NOT NULL                COMMENT '抽取后的纯文本（已归一化并按上限截断）',
    char_count     INT          NOT NULL                COMMENT '文本字符数',
    extract_status VARCHAR(20)  NOT NULL DEFAULT 'READY' COMMENT '抽取状态：READY/SKIPPED',
    error_msg      VARCHAR(1000) NULL    DEFAULT NULL   COMMENT '抽取失败原因（截断 1000 字符，成功为空）',
    create_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by      BIGINT       NULL     DEFAULT NULL   COMMENT '创建人ID',
    update_by      BIGINT       NULL     DEFAULT NULL   COMMENT '更新人ID',
    deleted        TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    version        INT          NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
    PRIMARY KEY (id),
    UNIQUE KEY uk_doc_text_file (file_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文档抽取文本（RAG 语料）';

-- ----------------------------------------------------------------------
-- 2. 文档分块（doc_chunk，一个文件多条，chunk_no 从 0 递增）
-- ----------------------------------------------------------------------
CREATE TABLE doc_chunk (
    id          BIGINT      NOT NULL              COMMENT '主键（雪花ID）',
    file_id     BIGINT      NOT NULL              COMMENT '文件ID（逻辑引用 doc_file.id）',
    chunk_no    INT         NOT NULL              COMMENT '分块序号（同一文件内从 0 递增）',
    content     TEXT        NOT NULL              COMMENT '分块文本',
    char_count  INT         NOT NULL              COMMENT '分块字符数',
    page_no     INT         NULL     DEFAULT NULL COMMENT '页码（M2 暂为空，预留给 M3 引用定位）',
    token_count INT         NULL     DEFAULT NULL COMMENT '分词数（M2 以字符数近似，M3 向量化可替换）',
    embedding_id VARCHAR(64) NULL    DEFAULT NULL COMMENT '向量ID（M3 预留，M2 不写入）',
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by   BIGINT      NULL     DEFAULT NULL COMMENT '创建人ID',
    update_by   BIGINT      NULL     DEFAULT NULL COMMENT '更新人ID',
    deleted     TINYINT     NOT NULL DEFAULT 0    COMMENT '逻辑删除：0未删除 1已删除',
    version     INT         NOT NULL DEFAULT 0    COMMENT '乐观锁版本号',
    PRIMARY KEY (id),
    KEY idx_doc_chunk_file (file_id, deleted),
    FULLTEXT KEY ft_doc_chunk_content (content) WITH PARSER ngram
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文档分块语料（全文检索）';

-- ----------------------------------------------------------------------
-- 3. 文档索引任务（doc_index_task，异步抽取与分块的执行流水）
-- ----------------------------------------------------------------------
CREATE TABLE doc_index_task (
    id          BIGINT        NOT NULL              COMMENT '主键（雪花ID）',
    file_id     BIGINT        NOT NULL              COMMENT '文件ID（逻辑引用 doc_file.id）',
    task_type   VARCHAR(20)   NOT NULL DEFAULT 'EXTRACT' COMMENT '任务类型：EXTRACT（抽取+分块）',
    version_no  INT           NOT NULL              COMMENT '触发索引时的文件版本号',
    status      VARCHAR(20)   NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/RUNNING/SUCCESS/FAILED/SKIPPED',
    retry_count INT           NOT NULL DEFAULT 0    COMMENT '重试次数（人工重跑累加）',
    error_msg   VARCHAR(1000) NULL     DEFAULT NULL COMMENT '失败原因（截断 1000 字符）',
    create_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by   BIGINT        NULL     DEFAULT NULL COMMENT '创建人ID',
    update_by   BIGINT        NULL     DEFAULT NULL COMMENT '更新人ID',
    deleted     TINYINT       NOT NULL DEFAULT 0    COMMENT '逻辑删除：0未删除 1已删除',
    version     INT           NOT NULL DEFAULT 0    COMMENT '乐观锁版本号',
    PRIMARY KEY (id),
    KEY idx_doc_index_task_status (status, id),
    KEY idx_doc_index_task_file (file_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文档索引任务';
