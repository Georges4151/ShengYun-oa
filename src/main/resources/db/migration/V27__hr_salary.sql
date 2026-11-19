-- ======================================================================
-- V27：HR 薪资台账与薪资变动申请（薪资主台账 / 变动申请单）
--
-- 说明：
--   1. 主键为雪花 ID（BIGINT），不 AUTO_INCREMENT，与其他业务表保持一致。
--   2. 全部金额字段以 _enc 列落库（VARCHAR(255)），由 SensitiveTypeHandler
--      做 AES-256-GCM 加解密，接口输出统一脱敏；表中不出现任何明文金额列。
--   3. MySQL 不支持部分唯一索引，故用「可空生成列 active_flag + 唯一键」
--      保证「每人仅一条 ACTIVE 薪资、可保留任意多条 HISTORY」：
--      active_flag 为 NULL 时不参与唯一约束。
--   4. 薪资变动走 BPM 审批，bpm_instance_id 为实例逻辑引用，不建物理外键。
--   5. 只做增量新增表，不修改任何历史迁移与既有业务表。
-- ======================================================================

-- ----------------------------------------------------------------------
-- 1. 薪资主台账（hr_salary）
-- ----------------------------------------------------------------------
CREATE TABLE hr_salary (
    id                   BIGINT       NOT NULL                COMMENT '主键（雪花ID）',
    employee_id          BIGINT       NOT NULL                COMMENT '员工ID（逻辑引用 hr_employee.id）',
    effective_date       DATE         NOT NULL                COMMENT '生效日期',
    base_salary_enc      VARCHAR(255) NULL     DEFAULT NULL   COMMENT '基本工资（加密存储）',
    post_salary_enc      VARCHAR(255) NULL     DEFAULT NULL   COMMENT '岗位工资（加密存储）',
    performance_base_enc VARCHAR(255) NULL     DEFAULT NULL   COMMENT '绩效基数（加密存储）',
    allowance_enc        VARCHAR(255) NULL     DEFAULT NULL   COMMENT '固定补贴（加密存储）',
    status               VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE/HISTORY',
    change_apply_id      BIGINT       NULL     DEFAULT NULL   COMMENT '来源变动申请ID（逻辑引用 hr_salary_change_apply.id）',
    remark               VARCHAR(500) NULL     DEFAULT NULL   COMMENT '备注',
    active_flag          TINYINT      GENERATED ALWAYS AS (CASE WHEN status = 'ACTIVE' THEN 1 ELSE NULL END) STORED COMMENT '生成列：ACTIVE为1，其余为NULL（用于唯一约束）',
    create_time          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by            BIGINT       NULL     DEFAULT NULL   COMMENT '创建人ID',
    update_by            BIGINT       NULL     DEFAULT NULL   COMMENT '更新人ID',
    deleted              TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    version              INT          NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
    PRIMARY KEY (id),
    UNIQUE KEY uk_hr_salary_active (employee_id, active_flag),
    KEY idx_hr_salary_emp (employee_id, effective_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='HR薪资台账';

-- ----------------------------------------------------------------------
-- 2. 薪资变动申请单（hr_salary_change_apply）
--    审批链路由 BPM 承载，status 只是申请单自身的业务状态快照。
-- ----------------------------------------------------------------------
CREATE TABLE hr_salary_change_apply (
    id               BIGINT       NOT NULL                COMMENT '主键（雪花ID）',
    employee_id      BIGINT       NOT NULL                COMMENT '目标员工ID（逻辑引用 hr_employee.id）',
    applicant_id     BIGINT       NULL     DEFAULT NULL   COMMENT '发起人ID（逻辑引用 sys_user.id）',
    change_type      VARCHAR(20)  NOT NULL                COMMENT '变动类型：ADJUST/PROMOTION/CORRECTION',
    before_snapshot  JSON         NULL     DEFAULT NULL   COMMENT '变更前快照（服务端脱敏后写入）',
    after_snapshot   JSON         NULL     DEFAULT NULL   COMMENT '变更后快照（服务端脱敏后写入）',
    effective_date   DATE         NULL     DEFAULT NULL   COMMENT '期望生效日期',
    reason           VARCHAR(500) NULL     DEFAULT NULL   COMMENT '申请原因',
    bpm_instance_id  BIGINT       NULL     DEFAULT NULL   COMMENT 'BPM流程实例ID（逻辑引用 bpm_instance.id）',
    status           VARCHAR(20)  NOT NULL DEFAULT 'DRAFT' COMMENT '状态：DRAFT/APPROVING/APPROVED/REJECTED',
    create_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by        BIGINT       NULL     DEFAULT NULL   COMMENT '创建人ID',
    update_by        BIGINT       NULL     DEFAULT NULL   COMMENT '更新人ID',
    deleted          TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    version          INT          NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
    PRIMARY KEY (id),
    KEY idx_hr_salary_change_emp (employee_id, status),
    KEY idx_hr_salary_change_bpm (bpm_instance_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='HR薪资变动申请单';
