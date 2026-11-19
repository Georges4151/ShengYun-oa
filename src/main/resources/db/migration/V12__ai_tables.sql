-- ======================================================================
-- V12__ai_tables.sql
-- 本地 Ollama 智能化 M1：AI 审计三表（会话 / 消息 / 反馈）。
--
-- 说明：
--   1. 主键为应用侧生成的雪花 ID（BIGINT，非 AUTO_INCREMENT）。
--   2. 全部沿用 BaseEntity 审计列：create_time/update_time/create_by/update_by/
--      deleted/version；逻辑删除 0未删除 1已删除。
--   3. 统一 utf8mb4 / InnoDB；仅建普通索引，不建物理外键（逻辑引用）。
--   4. tool_calls 仅存工具名、参数与返回行数等 JSON 文本，不存全量业务数据。
-- ======================================================================

-- 1. AI 会话
CREATE TABLE ai_conversation (
    id          BIGINT       NOT NULL                COMMENT '主键（雪花ID）',
    user_id     BIGINT       NOT NULL                COMMENT '所属用户ID（逻辑引用 sys_user.id）',
    title       VARCHAR(200) NULL     DEFAULT NULL   COMMENT '会话标题',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by   BIGINT       NULL     DEFAULT NULL   COMMENT '创建人ID',
    update_by   BIGINT       NULL     DEFAULT NULL   COMMENT '更新人ID',
    deleted     TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    version     INT          NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
    PRIMARY KEY (id),
    KEY idx_ai_conv_user (user_id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 会话';

-- 2. AI 单条消息
CREATE TABLE ai_message (
    id                BIGINT      NOT NULL                COMMENT '主键（雪花ID）',
    conversation_id   BIGINT      NOT NULL                COMMENT '所属会话ID（逻辑引用 ai_conversation.id）',
    role              VARCHAR(20) NOT NULL                COMMENT '角色：USER/ASSISTANT/TOOL',
    content           LONGTEXT    NULL                    COMMENT '消息内容',
    tool_calls        TEXT        NULL                    COMMENT '工具调用记录（JSON 文本）',
    model             VARCHAR(80) NULL     DEFAULT NULL   COMMENT '模型名',
    prompt_tokens     INT         NULL     DEFAULT NULL   COMMENT '提示词 token 数',
    completion_tokens INT         NULL     DEFAULT NULL   COMMENT '生成 token 数',
    latency_ms        INT         NULL     DEFAULT NULL   COMMENT '耗时（毫秒）',
    create_time       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by         BIGINT      NULL     DEFAULT NULL   COMMENT '创建人ID',
    update_by         BIGINT      NULL     DEFAULT NULL   COMMENT '更新人ID',
    deleted           TINYINT     NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    version           INT         NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
    PRIMARY KEY (id),
    KEY idx_ai_msg_conv (conversation_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 消息';

-- 3. AI 用户反馈
CREATE TABLE ai_feedback (
    id          BIGINT       NOT NULL                COMMENT '主键（雪花ID）',
    message_id  BIGINT       NOT NULL                COMMENT '被评价消息ID（逻辑引用 ai_message.id）',
    user_id     BIGINT       NOT NULL                COMMENT '评价用户ID（逻辑引用 sys_user.id）',
    score       TINYINT      NOT NULL                COMMENT '评分：1~5',
    remark      VARCHAR(500) NULL     DEFAULT NULL   COMMENT '备注',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by   BIGINT       NULL     DEFAULT NULL   COMMENT '创建人ID',
    update_by   BIGINT       NULL     DEFAULT NULL   COMMENT '更新人ID',
    deleted     TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    version     INT          NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
    PRIMARY KEY (id),
    KEY idx_ai_fb_msg (message_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 用户反馈';
