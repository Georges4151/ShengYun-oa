-- ======================================================================
-- V35：行政后勤 - 访客与报修（访客登记 / 报修工单）
--
-- 说明：
--   1. 主键为雪花 ID（BIGINT），不 AUTO_INCREMENT。
--   2. 访客手机号走 SensitiveTypeHandler 加密（字段名 *_enc），接口输出统一脱敏。
--   3. qr_token 为一次性核销令牌，核销后置 NULL；MySQL 唯一键允许多个 NULL。
--   4. 报修工单补 REJECTED 态，由审批驳回回调写入。
--   5. 仅增量新增表，不修改任何历史迁移与既有业务表。
-- ======================================================================

-- ----------------------------------------------------------------------
-- 10. 访客登记（admin_visitor）
-- ----------------------------------------------------------------------
CREATE TABLE admin_visitor (
    id            BIGINT       NOT NULL                COMMENT '主键（雪花ID）',
    visitor_name  VARCHAR(64)  NOT NULL                COMMENT '访客姓名',
    phone_enc     VARCHAR(255) NULL     DEFAULT NULL   COMMENT '手机号（库内加密，接口脱敏）',
    company       VARCHAR(128) NULL     DEFAULT NULL   COMMENT '来访单位',
    host_user_id  BIGINT       NOT NULL                COMMENT '被访人ID（sys_user.id）',
    visit_date    DATE         NOT NULL                COMMENT '来访日期',
    purpose       VARCHAR(255) NULL     DEFAULT NULL   COMMENT '来访事由',
    qr_token      VARCHAR(64)  NULL     DEFAULT NULL   COMMENT '一次性核销令牌（核销后置NULL）',
    status        VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/CHECKED_IN/CHECKED_OUT/CANCELLED',
    checkin_time  DATETIME     NULL     DEFAULT NULL   COMMENT '进入时间',
    checkout_time DATETIME     NULL     DEFAULT NULL   COMMENT '离开时间',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by     BIGINT       NULL     DEFAULT NULL   COMMENT '创建人ID',
    update_by     BIGINT       NULL     DEFAULT NULL   COMMENT '更新人ID',
    deleted       TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    version       INT          NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
    PRIMARY KEY (id),
    UNIQUE KEY uk_admin_visitor_token (qr_token),
    KEY idx_admin_visitor_date (visit_date, status),
    KEY idx_admin_visitor_host (host_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='行政访客登记';

-- ----------------------------------------------------------------------
-- 11. 报修工单（admin_repair）
-- ----------------------------------------------------------------------
CREATE TABLE admin_repair (
    id              BIGINT        NOT NULL                COMMENT '主键（雪花ID）',
    repair_no       VARCHAR(64)   NOT NULL                COMMENT '工单号（RP-yyyyMMdd-####）',
    reporter_id     BIGINT        NOT NULL                COMMENT '报修人ID（sys_user.id）',
    dept_id         BIGINT        NULL     DEFAULT NULL   COMMENT '报修人部门快照',
    location        VARCHAR(128)  NULL     DEFAULT NULL   COMMENT '报修位置',
    category        VARCHAR(32)   NOT NULL DEFAULT 'OTHER' COMMENT '分类：ELECTRIC/WATER/FURNITURE/IT/OTHER',
    description     VARCHAR(1000) NULL     DEFAULT NULL   COMMENT '故障描述',
    images_json     JSON          NULL     DEFAULT NULL   COMMENT '图片文件名数组',
    assignee_id     BIGINT        NULL     DEFAULT NULL   COMMENT '维修人ID',
    bpm_instance_id BIGINT        NULL     DEFAULT NULL   COMMENT 'BPM实例ID',
    status          VARCHAR(20)   NOT NULL DEFAULT 'SUBMITTED' COMMENT '状态：SUBMITTED/ASSIGNED/PROCESSING/DONE/ACCEPTED/REJECTED',
    assigned_at     DATETIME      NULL     DEFAULT NULL   COMMENT '派单时间',
    processed_at    DATETIME      NULL     DEFAULT NULL   COMMENT '开始处理时间',
    finished_at     DATETIME      NULL     DEFAULT NULL   COMMENT '完工时间',
    accepted_at     DATETIME      NULL     DEFAULT NULL   COMMENT '验收时间',
    remark          VARCHAR(500)  NULL     DEFAULT NULL   COMMENT '处理说明',
    create_time     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by       BIGINT        NULL     DEFAULT NULL   COMMENT '创建人ID',
    update_by       BIGINT        NULL     DEFAULT NULL   COMMENT '更新人ID',
    deleted         TINYINT       NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    version         INT           NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
    PRIMARY KEY (id),
    UNIQUE KEY uk_admin_repair_no (repair_no),
    KEY idx_admin_repair_status (status),
    KEY idx_admin_repair_assignee (assignee_id, status),
    KEY idx_admin_repair_bpm (bpm_instance_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='行政报修工单';
