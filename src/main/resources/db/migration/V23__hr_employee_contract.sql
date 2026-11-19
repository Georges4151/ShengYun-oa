-- ======================================================================
-- V23：HR 人事基础表（岗位 / 员工档案 / 劳动合同 / 入转调离申请单）
--
-- 说明：
--   1. 主键为雪花 ID（BIGINT），不 AUTO_INCREMENT，与其他业务表保持一致。
--   2. 敏感字段（身份证、手机号、紧急联系电话）以 _enc 列落库，
--      由 SensitiveTypeHandler 做 AES-256-GCM 加解密，接口输出统一脱敏。
--   3. 员工、合同、申请单之间的关联均为逻辑引用，不建物理外键。
--   4. 只做增量新增表，不修改任何历史迁移与既有业务表。
-- ======================================================================

-- ----------------------------------------------------------------------
-- 1. 岗位（hr_position）
-- ----------------------------------------------------------------------
CREATE TABLE hr_position (
    id          BIGINT       NOT NULL                COMMENT '主键（雪花ID）',
    code        VARCHAR(64)  NOT NULL                COMMENT '岗位编码（唯一）',
    name        VARCHAR(128) NOT NULL                COMMENT '岗位名称',
    level       VARCHAR(32)  NULL     DEFAULT NULL   COMMENT '岗位职级（如 P5/M2）',
    job_family  VARCHAR(64)  NULL     DEFAULT NULL   COMMENT '岗位序列（技术/管理/职能等）',
    sort_order  INT          NOT NULL DEFAULT 0      COMMENT '排序号（升序）',
    status      VARCHAR(20)  NOT NULL DEFAULT 'ENABLED' COMMENT '状态：ENABLED/DISABLED',
    remark      VARCHAR(255) NULL     DEFAULT NULL   COMMENT '备注',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by   BIGINT       NULL     DEFAULT NULL   COMMENT '创建人ID',
    update_by   BIGINT       NULL     DEFAULT NULL   COMMENT '更新人ID',
    deleted     TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    version     INT          NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
    PRIMARY KEY (id),
    UNIQUE KEY uk_hr_position_code (code),
    KEY idx_hr_position_status (status, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='HR岗位';

-- ----------------------------------------------------------------------
-- 2. 员工档案（hr_employee）
-- ----------------------------------------------------------------------
CREATE TABLE hr_employee (
    id                 BIGINT       NOT NULL                COMMENT '主键（雪花ID）',
    user_id            BIGINT       NULL     DEFAULT NULL   COMMENT '绑定系统用户ID（逻辑引用 sys_user.id，唯一）',
    employee_no        VARCHAR(64)  NOT NULL                COMMENT '员工工号（唯一）',
    name               VARCHAR(64)  NOT NULL                COMMENT '姓名',
    gender             VARCHAR(16)  NULL     DEFAULT NULL   COMMENT '性别：MALE/FEMALE/UNKNOWN',
    birth_date         DATE         NULL     DEFAULT NULL   COMMENT '出生日期',
    id_card_enc        VARCHAR(255) NULL     DEFAULT NULL   COMMENT '身份证号（加密存储）',
    phone_enc          VARCHAR(255) NULL     DEFAULT NULL   COMMENT '手机号（加密存储）',
    email              VARCHAR(128) NULL     DEFAULT NULL   COMMENT '邮箱（接口脱敏）',
    dept_id            BIGINT       NULL     DEFAULT NULL   COMMENT '所属部门ID（逻辑引用 sys_dept.id）',
    position_id        BIGINT       NULL     DEFAULT NULL   COMMENT '岗位ID（逻辑引用 hr_position.id）',
    employment_type    VARCHAR(20)  NULL     DEFAULT NULL   COMMENT '用工类型：FULL_TIME/PART_TIME/INTERN/CONTRACTOR',
    hire_date          DATE         NULL     DEFAULT NULL   COMMENT '入职日期',
    probation_end_date DATE         NULL     DEFAULT NULL   COMMENT '试用期结束日期',
    regular_date       DATE         NULL     DEFAULT NULL   COMMENT '转正日期',
    leave_date         DATE         NULL     DEFAULT NULL   COMMENT '离职日期',
    status             VARCHAR(20)  NOT NULL DEFAULT 'PROBATION' COMMENT '状态：PROBATION/ACTIVE/LEAVE/TERMINATED',
    emergency_contact  VARCHAR(64)  NULL     DEFAULT NULL   COMMENT '紧急联系人',
    emergency_phone_enc VARCHAR(255) NULL    DEFAULT NULL   COMMENT '紧急联系电话（加密存储）',
    id_card_file_id    BIGINT       NULL     DEFAULT NULL   COMMENT '身份证附件ID（逻辑引用文件表）',
    remark             VARCHAR(500) NULL     DEFAULT NULL   COMMENT '备注',
    create_time        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by          BIGINT       NULL     DEFAULT NULL   COMMENT '创建人ID',
    update_by          BIGINT       NULL     DEFAULT NULL   COMMENT '更新人ID',
    deleted            TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    version            INT          NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
    PRIMARY KEY (id),
    UNIQUE KEY uk_hr_employee_no (employee_no),
    UNIQUE KEY uk_hr_employee_user (user_id),
    -- employee_no 已有唯一索引，不再重复建普通索引
    KEY idx_hr_employee_dept_status (dept_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='HR员工档案';

-- ----------------------------------------------------------------------
-- 3. 劳动合同（hr_contract）
-- ----------------------------------------------------------------------
CREATE TABLE hr_contract (
    id                  BIGINT       NOT NULL                COMMENT '主键（雪花ID）',
    employee_id         BIGINT       NOT NULL                COMMENT '员工ID（逻辑引用 hr_employee.id）',
    contract_no         VARCHAR(64)  NOT NULL                COMMENT '合同编号（唯一）',
    contract_type       VARCHAR(20)  NOT NULL                COMMENT '合同类型：FIXED/UNFIXED/INTERN',
    start_date          DATE         NULL     DEFAULT NULL   COMMENT '合同开始日期',
    end_date            DATE         NULL     DEFAULT NULL   COMMENT '合同结束日期',
    sign_date           DATE         NULL     DEFAULT NULL   COMMENT '签订日期',
    file_id             BIGINT       NULL     DEFAULT NULL   COMMENT '合同附件ID（逻辑引用文件表）',
    status              VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE/EXPIRED/TERMINATED',
    renewal_remind_date DATE         NULL     DEFAULT NULL   COMMENT '续签提醒日期',
    remark              VARCHAR(500) NULL     DEFAULT NULL   COMMENT '备注',
    create_time         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by           BIGINT       NULL     DEFAULT NULL   COMMENT '创建人ID',
    update_by           BIGINT       NULL     DEFAULT NULL   COMMENT '更新人ID',
    deleted             TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    version             INT          NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
    PRIMARY KEY (id),
    UNIQUE KEY uk_hr_contract_no (contract_no),
    KEY idx_hr_contract_employee_status (employee_id, status),
    KEY idx_hr_contract_end_date (end_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='HR劳动合同';

-- ----------------------------------------------------------------------
-- 4. 入转调离申请单（hr_employee_change）
--    审批链路由 BPM 承载，approval_status 只是实例状态的业务快照。
-- ----------------------------------------------------------------------
CREATE TABLE hr_employee_change (
    id                  BIGINT       NOT NULL                COMMENT '主键（雪花ID）',
    employee_id         BIGINT       NOT NULL                COMMENT '员工ID（逻辑引用 hr_employee.id）',
    change_type         VARCHAR(20)  NOT NULL                COMMENT '类型：REGULAR/TRANSFER/RESIGN',
    target_dept_id      BIGINT       NULL     DEFAULT NULL   COMMENT '调岗目标部门ID（TRANSFER 使用）',
    target_position_id  BIGINT       NULL     DEFAULT NULL   COMMENT '调岗目标岗位ID（TRANSFER 使用）',
    effective_date      DATE         NULL     DEFAULT NULL   COMMENT '生效日期',
    reason              VARCHAR(500) NULL     DEFAULT NULL   COMMENT '申请原因',
    approval_status     VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT '审批状态：PENDING/APPROVED/REJECTED/WITHDRAWN/CANCELLED',
    bpm_instance_id     BIGINT       NULL     DEFAULT NULL   COMMENT 'BPM流程实例ID（逻辑引用 bpm_instance.id）',
    remark              VARCHAR(500) NULL     DEFAULT NULL   COMMENT '备注',
    create_time         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by           BIGINT       NULL     DEFAULT NULL   COMMENT '创建人ID',
    update_by           BIGINT       NULL     DEFAULT NULL   COMMENT '更新人ID',
    deleted             TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    version             INT          NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
    PRIMARY KEY (id),
    KEY idx_hr_change_employee_type (employee_id, change_type),
    KEY idx_hr_change_instance (bpm_instance_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='HR入转调离申请单';
