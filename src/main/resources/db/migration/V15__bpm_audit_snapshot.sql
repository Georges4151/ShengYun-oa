-- ======================================================================
-- V15：业务变更审计快照表（M1）
--
-- 说明：
--   1. 现有 sys_operation_log 不含变更前后数据，本表承接审批动作与
--      模板发布的前后 JSON 快照，为后续薪资等敏感模块预留脱敏清单。
--   2. before_json / after_json 使用 LONGTEXT，避免大对象截断。
--   3. 仅新增表，不修改现有审计表。
-- ======================================================================

CREATE TABLE biz_audit_snapshot (
    id            BIGINT       NOT NULL                COMMENT '主键（雪花ID）',
    target_type   VARCHAR(64)  NOT NULL                COMMENT '目标类型：BPM_TEMPLATE/BPM_INSTANCE/BPM_TASK',
    target_id     BIGINT       NULL     DEFAULT NULL   COMMENT '目标ID（逻辑引用，不建物理外键）',
    action        VARCHAR(40)  NOT NULL                COMMENT '动作：START/APPROVE/REJECT/WITHDRAW/TERMINATE/DELEGATE/TEMPLATE_PUBLISH',
    before_json   LONGTEXT     NULL                    COMMENT '变更前 JSON 快照',
    after_json    LONGTEXT     NULL                    COMMENT '变更后 JSON 快照',
    masked_fields VARCHAR(500) NULL     DEFAULT NULL   COMMENT '脱敏字段清单（英文逗号分隔）',
    operator_id   BIGINT       NULL     DEFAULT NULL   COMMENT '操作人ID（逻辑引用 sys_user.id）',
    trace_id      VARCHAR(64)  NULL     DEFAULT NULL   COMMENT '链路追踪ID',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by     BIGINT       NULL     DEFAULT NULL   COMMENT '创建人ID',
    update_by     BIGINT       NULL     DEFAULT NULL   COMMENT '更新人ID',
    deleted       TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    version       INT          NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
    PRIMARY KEY (id),
    KEY idx_biz_audit_snapshot_target (target_type, target_id, create_time),
    KEY idx_biz_audit_snapshot_action (action, create_time),
    KEY idx_biz_audit_snapshot_trace (trace_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='业务变更审计快照';
