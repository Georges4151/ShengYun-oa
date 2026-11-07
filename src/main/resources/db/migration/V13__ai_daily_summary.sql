-- ======================================================================
-- V13__ai_daily_summary.sql
-- 本地 Ollama 智能化 M3：AI 经营日报摘要表。
--
-- 说明：
--   1. 主键为应用侧生成的雪花 ID（BIGINT，非 AUTO_INCREMENT）。
--   2. 沿用 BaseEntity 审计列：create_time/update_time/create_by/update_by/
--      deleted/version；逻辑删除 0未删除 1已删除。
--   3. 统一 utf8mb4 / InnoDB；仅建普通索引，不建物理外键（逻辑引用）。
--   4. 同一业务日同一周期仅保留一条摘要，重复生成走覆盖更新。
-- ======================================================================

CREATE TABLE ai_daily_summary (
    id              BIGINT       NOT NULL                COMMENT '主键（雪花ID）',
    summary_date    DATE         NOT NULL                COMMENT '摘要日期（Asia/Shanghai 业务日）',
    period_type     VARCHAR(16)  NOT NULL DEFAULT 'DAILY' COMMENT '周期类型：DAILY',
    content         TEXT         NOT NULL                COMMENT '最终展示文本',
    metrics_json    TEXT         NULL     DEFAULT NULL   COMMENT '结构化指标（JSON 文本）',
    model           VARCHAR(64)  NULL     DEFAULT NULL   COMMENT '生成模型：TEMPLATE 或实际模型名',
    trigger_type    VARCHAR(16)  NOT NULL                COMMENT '触发方式：SCHEDULED/MANUAL',
    notification_id BIGINT       NULL     DEFAULT NULL   COMMENT '关联通知ID（逻辑引用 biz_notification.id）',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by       BIGINT       NULL     DEFAULT NULL   COMMENT '创建人ID',
    update_by       BIGINT       NULL     DEFAULT NULL   COMMENT '更新人ID',
    deleted         TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    version         INT          NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ai_daily_summary_date (summary_date, period_type),
    KEY idx_ai_daily_summary_notification (notification_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 经营日报摘要';
