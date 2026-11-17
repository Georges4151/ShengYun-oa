-- ======================================================================
-- V22：BPM 能力增强（用户自助代理 + 节点自动通过）
--
-- 说明：
--   1. bpm_delegate 保存用户自助代理关系；任务生成时命中生效代理，
--      assignee_id 写代理人，owner_assignee_id 保留原审批人。
--   2. bpm_task 增加 owner_assignee_id，用于展示“代理人（代 XXX）”。
--   3. 节点自动通过不需要新表：模板 JSON 的 autoApproveIfInitiator /
--      autoApproveWhen 由 BpmSchemaParser 解析，命中时直接完成节点。
--   4. 只做增量变更，不修改历史迁移。
-- ======================================================================

ALTER TABLE bpm_task
    ADD COLUMN owner_assignee_id BIGINT NULL DEFAULT NULL
        COMMENT '被代理人ID（NULL=未代理；有值表示 assignee_id 为代理处理人）'
        AFTER assignee_id;

CREATE TABLE bpm_delegate (
    id          BIGINT       NOT NULL                COMMENT '主键（雪花ID）',
    user_id     BIGINT       NOT NULL                COMMENT '委托人ID（逻辑引用 sys_user.id）',
    delegate_id BIGINT       NOT NULL                COMMENT '代理人ID（逻辑引用 sys_user.id）',
    biz_type    VARCHAR(64)  NULL     DEFAULT NULL   COMMENT '适用范围：NULL=全部业务；EXPENSE/CHANGE_REQUEST=指定业务',
    start_time  DATETIME     NOT NULL                COMMENT '代理开始时间',
    end_time    DATETIME     NOT NULL                COMMENT '代理结束时间',
    status      VARCHAR(20)  NOT NULL DEFAULT 'ENABLED' COMMENT '状态：ENABLED/DISABLED',
    reason      VARCHAR(255) NULL     DEFAULT NULL   COMMENT '代理原因',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by   BIGINT       NULL     DEFAULT NULL   COMMENT '创建人ID',
    update_by   BIGINT       NULL     DEFAULT NULL   COMMENT '更新人ID',
    deleted     TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    version     INT          NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
    PRIMARY KEY (id),
    KEY idx_bpm_delegate_user_time (user_id, status, start_time, end_time),
    KEY idx_bpm_delegate_delegate (delegate_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='BPM用户代理关系';
