-- ======================================================================
-- V29：HR 月度薪资核算台账与工资条
--
-- 说明：
--   1. 主键为雪花 ID（BIGINT），不 AUTO_INCREMENT，与其他业务表保持一致。
--   2. 全部金额字段以 _enc 列落库（VARCHAR(255)），由 SensitiveTypeHandler
--      做 AES-256-GCM 加解密；计算过程用 DECIMAL(18,2) + HALF_UP，落库即加密。
--   3. 考勤天数、迟到次数与绩效系数为业务度量值，按 DECIMAL/INT 明文存储。
--   4. 工资条明细 content_json 中的金额同样先加密再序列化，避免明文明细泄露。
--   5. 核算表带状态与乐观锁：DRAFT 可改，CONFIRMED 不可改，LOCKED 跳过重算。
--   6. 只做增量新增表，不修改任何历史迁移与既有业务表。
-- ======================================================================

-- ----------------------------------------------------------------------
-- 1. 月度薪资核算台账（hr_salary_calc）
-- ----------------------------------------------------------------------
CREATE TABLE hr_salary_calc (
    id                        BIGINT       NOT NULL                COMMENT '主键（雪花ID）',
    employee_id               BIGINT       NOT NULL                COMMENT '员工ID（逻辑引用 hr_employee.id）',
    calc_month                VARCHAR(7)   NOT NULL                COMMENT '核算月（如 2026-11）',
    base_salary_enc           VARCHAR(255) NULL     DEFAULT NULL   COMMENT '基本工资（取自生效薪资，加密存储）',
    post_salary_enc           VARCHAR(255) NULL     DEFAULT NULL   COMMENT '岗位工资（取自生效薪资，加密存储）',
    attendance_days           DECIMAL(5,2) NOT NULL DEFAULT 0.00   COMMENT '应出勤天数',
    absent_days               DECIMAL(5,2) NOT NULL DEFAULT 0.00   COMMENT '缺勤天数',
    late_count                INT          NOT NULL DEFAULT 0      COMMENT '迟到/早退次数',
    attendance_deduction_enc  VARCHAR(255) NULL     DEFAULT NULL   COMMENT '考勤扣款（加密存储）',
    performance_coefficient   DECIMAL(6,4) NOT NULL DEFAULT 1.0000 COMMENT '绩效系数，默认 1.0',
    performance_amount_enc    VARCHAR(255) NULL     DEFAULT NULL   COMMENT '绩效工资（加密存储）',
    allowance_enc             VARCHAR(255) NULL     DEFAULT NULL   COMMENT '补贴（加密存储）',
    social_insurance_enc      VARCHAR(255) NULL     DEFAULT NULL   COMMENT '社保（加密存储）',
    housing_fund_enc          VARCHAR(255) NULL     DEFAULT NULL   COMMENT '公积金（加密存储）',
    tax_enc                   VARCHAR(255) NULL     DEFAULT NULL   COMMENT '个人所得税（加密存储）',
    gross_pay_enc             VARCHAR(255) NULL     DEFAULT NULL   COMMENT '应发合计（加密存储）',
    net_pay_enc               VARCHAR(255) NULL     DEFAULT NULL   COMMENT '实发合计（加密存储）',
    status                    VARCHAR(20)  NOT NULL DEFAULT 'DRAFT' COMMENT '状态：DRAFT/CONFIRMED/LOCKED',
    confirmed_by              BIGINT       NULL     DEFAULT NULL   COMMENT '确认人ID（逻辑引用 sys_user.id）',
    confirmed_at              DATETIME     NULL     DEFAULT NULL   COMMENT '确认时间',
    create_time               DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time               DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by                 BIGINT       NULL     DEFAULT NULL   COMMENT '创建人ID',
    update_by                 BIGINT       NULL     DEFAULT NULL   COMMENT '更新人ID',
    deleted                   TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    version                   INT          NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
    PRIMARY KEY (id),
    UNIQUE KEY uk_hr_calc (employee_id, calc_month),
    KEY idx_hr_calc_month (calc_month, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='HR月度薪资核算台账';

-- ----------------------------------------------------------------------
-- 2. 工资条（hr_salary_slip）
-- ----------------------------------------------------------------------
CREATE TABLE hr_salary_slip (
    id            BIGINT       NOT NULL                COMMENT '主键（雪花ID）',
    employee_id   BIGINT       NOT NULL                COMMENT '员工ID（逻辑引用 hr_employee.id）',
    calc_id       BIGINT       NULL     DEFAULT NULL   COMMENT '来源核算记录ID（逻辑引用 hr_salary_calc.id）',
    calc_month    VARCHAR(7)   NOT NULL                COMMENT '核算月（如 2026-11）',
    content_json  TEXT         NULL     DEFAULT NULL   COMMENT '工资条明细（金额加密后序列化）',
    published_at  DATETIME     NULL     DEFAULT NULL   COMMENT '发布时间',
    read_status   VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT '阅读状态：PENDING/READ',
    read_at       DATETIME     NULL     DEFAULT NULL   COMMENT '阅读时间',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by     BIGINT       NULL     DEFAULT NULL   COMMENT '创建人ID',
    update_by     BIGINT       NULL     DEFAULT NULL   COMMENT '更新人ID',
    deleted       TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    version       INT          NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
    PRIMARY KEY (id),
    UNIQUE KEY uk_hr_slip (employee_id, calc_month),
    KEY idx_hr_slip_month (calc_month)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='HR工资条';
