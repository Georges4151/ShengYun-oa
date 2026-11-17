-- ======================================================================
-- V40：企业微信集成 - 组织同步批次 / 应用消息发件箱 / 用户与部门映射列
--
-- 说明：
--   1. 主键为雪花 ID（BIGINT），不 AUTO_INCREMENT。
--   2. 本脚本仅建表与加列，权限点由 V41__wecom_permission_seed.sql 负责；
--      两脚本按 V40 → V41 顺序执行，V41 依赖本脚本（逻辑引用，不建外键）。
--   3. integration_wecom_sync 记录每次组织同步（部门 + 成员）的批次结果，
--      sync_batch_no 唯一，作为幂等与审计依据；status 由 RUNNING 单向流转。
--   4. integration_wecom_message 为应用消息发件箱：站内通知落库后按需入队，
--      由后台任务异步投递企业微信，失败按 next_retry_at 重试；
--      同一通知对同一用户仅入队一次（notification_id + target_user_id 唯一）。
--   5. sys_user.wecom_userid / sys_dept.wecom_dept_id 增加唯一键用于同步比对；
--      MySQL 唯一索引允许多个 NULL，故历史数据（未绑定企业微信）不受影响。
--   6. 本脚本为增量迁移，入库后不得修改；如需变更另起更高版本号脚本。
-- ======================================================================

-- ----------------------------------------------------------------------
-- 1. 企业微信组织同步批次（integration_wecom_sync）
-- ----------------------------------------------------------------------
CREATE TABLE integration_wecom_sync (
    id            BIGINT        NOT NULL                 COMMENT '主键（雪花ID）',
    sync_batch_no VARCHAR(32)   NOT NULL                 COMMENT '同步批次号（唯一）',
    sync_type     VARCHAR(16)   NOT NULL                 COMMENT '触发方式：MANUAL/SCHEDULED',
    total         INT           NOT NULL DEFAULT 0       COMMENT '同步对象总数（部门 + 成员）',
    success_count INT           NOT NULL DEFAULT 0       COMMENT '成功数',
    failed_count  INT           NOT NULL DEFAULT 0       COMMENT '失败数',
    status        VARCHAR(16)   NOT NULL DEFAULT 'RUNNING' COMMENT '状态：RUNNING/SUCCESS/PARTIAL/FAILED',
    error_msg     VARCHAR(1000) NULL     DEFAULT NULL    COMMENT '失败原因（截断 1000 字符）',
    started_at    DATETIME      NULL     DEFAULT NULL    COMMENT '开始时间',
    finished_at   DATETIME      NULL     DEFAULT NULL    COMMENT '结束时间',
    create_time   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by     BIGINT        NULL     DEFAULT NULL    COMMENT '创建人ID',
    update_by     BIGINT        NULL     DEFAULT NULL    COMMENT '更新人ID',
    deleted       TINYINT       NOT NULL DEFAULT 0       COMMENT '逻辑删除：0未删除 1已删除',
    version       INT           NOT NULL DEFAULT 0       COMMENT '乐观锁版本号',
    PRIMARY KEY (id),
    UNIQUE KEY uk_wecom_sync_batch (sync_batch_no),
    KEY idx_wecom_sync_status (status, started_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='企业微信组织同步批次';

-- ----------------------------------------------------------------------
-- 2. 企业微信应用消息发件箱（integration_wecom_message）
-- ----------------------------------------------------------------------
CREATE TABLE integration_wecom_message (
    id              BIGINT       NOT NULL                 COMMENT '主键（雪花ID）',
    notification_id BIGINT       NOT NULL                 COMMENT '站内通知ID（逻辑引用 biz_notification.id）',
    target_user_id  BIGINT       NOT NULL                 COMMENT '目标用户ID（逻辑引用 sys_user.id）',
    wecom_userid    VARCHAR(64)  NOT NULL                 COMMENT '企业微信成员账号（入队时快照）',
    msg_type        VARCHAR(16)  NOT NULL DEFAULT 'TEXT'  COMMENT '消息类型：当前仅 TEXT',
    payload_json    JSON         NULL     DEFAULT NULL    COMMENT '消息体快照（title/content/linkUrl）',
    status          VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/SENT/FAILED',
    retry_count     INT          NOT NULL DEFAULT 0       COMMENT '已重试次数',
    next_retry_at   DATETIME     NULL     DEFAULT NULL    COMMENT '下次重试时间（PENDING 时生效）',
    error_msg       VARCHAR(500) NULL     DEFAULT NULL    COMMENT '最近一次失败原因',
    sent_at         DATETIME     NULL     DEFAULT NULL    COMMENT '发送成功时间',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by       BIGINT       NULL     DEFAULT NULL    COMMENT '创建人ID',
    update_by       BIGINT       NULL     DEFAULT NULL    COMMENT '更新人ID',
    deleted         TINYINT      NOT NULL DEFAULT 0       COMMENT '逻辑删除：0未删除 1已删除',
    version         INT          NOT NULL DEFAULT 0       COMMENT '乐观锁版本号',
    PRIMARY KEY (id),
    UNIQUE KEY uk_wecom_msg_notification_user (notification_id, target_user_id),
    KEY idx_wecom_msg_status_retry (status, next_retry_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='企业微信应用消息发件箱';

-- ----------------------------------------------------------------------
-- 3. 用户企业微信映射（sys_user）
--
--    MySQL 唯一索引允许多个 NULL，因此未绑定企业微信的历史账号
--    （wecom_userid 为 NULL）不参与唯一性冲突，加唯一键不影响存量数据。
-- ----------------------------------------------------------------------
ALTER TABLE sys_user
    ADD COLUMN wecom_userid VARCHAR(64) NULL DEFAULT NULL COMMENT '企业微信成员账号（唯一，手工本地账号为 NULL）' AFTER leader_id,
    ADD UNIQUE KEY uk_sys_user_wecom (wecom_userid);

-- ----------------------------------------------------------------------
-- 4. 部门企业微信映射（sys_dept）
--
--    同样依赖 MySQL 唯一索引允许多个 NULL 的特性：未同步部门
--    （wecom_dept_id 为 NULL）不产生唯一性冲突。
-- ----------------------------------------------------------------------
ALTER TABLE sys_dept
    ADD COLUMN wecom_dept_id VARCHAR(64) NULL DEFAULT NULL COMMENT '企业微信部门ID（唯一，未同步部门为 NULL）',
    ADD UNIQUE KEY uk_sys_dept_wecom (wecom_dept_id);
