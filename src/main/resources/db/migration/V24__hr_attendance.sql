-- ======================================================================
-- V24：HR 考勤与请假（额度 / 排班 / 打卡 / 请假单 / 补卡单）
--
-- 说明：
--   1. 主键为雪花 ID（BIGINT），不 AUTO_INCREMENT。
--   2. 请假单、补卡单走 BPM 审批，approval_status 为实例状态快照。
--   3. 打卡记录保留定位与 WiFi 信息，本期不做强制校验，仅留痕。
--   4. 只做增量新增表，不修改任何历史迁移与既有业务表。
-- ======================================================================

-- ----------------------------------------------------------------------
-- 1. 假期额度（hr_leave_quota）
-- ----------------------------------------------------------------------
CREATE TABLE hr_leave_quota (
    id             BIGINT       NOT NULL                COMMENT '主键（雪花ID）',
    employee_id    BIGINT       NOT NULL                COMMENT '员工ID（逻辑引用 hr_employee.id）',
    year           INT          NOT NULL                COMMENT '额度年度',
    leave_type     VARCHAR(20)  NOT NULL                COMMENT '假期类型：ANNUAL/SICK/PERSONAL',
    total_days     DECIMAL(6,1) NOT NULL DEFAULT 0.0    COMMENT '总额度（天）',
    used_days      DECIMAL(6,1) NOT NULL DEFAULT 0.0    COMMENT '已使用（天）',
    remaining_days DECIMAL(6,1) NOT NULL DEFAULT 0.0    COMMENT '剩余额度（天）',
    expire_date    DATE         NULL     DEFAULT NULL   COMMENT '额度失效日期',
    create_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by      BIGINT       NULL     DEFAULT NULL   COMMENT '创建人ID',
    update_by      BIGINT       NULL     DEFAULT NULL   COMMENT '更新人ID',
    deleted        TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    version        INT          NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
    PRIMARY KEY (id),
    UNIQUE KEY uk_hr_leave_quota (employee_id, year, leave_type),
    KEY idx_hr_leave_quota_year (year, leave_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='HR假期额度';

-- ----------------------------------------------------------------------
-- 2. 排班（hr_schedule）
-- ----------------------------------------------------------------------
CREATE TABLE hr_schedule (
    id            BIGINT       NOT NULL                COMMENT '主键（雪花ID）',
    employee_id   BIGINT       NOT NULL                COMMENT '员工ID（逻辑引用 hr_employee.id）',
    work_date     DATE         NOT NULL                COMMENT '排班日期',
    shift_code    VARCHAR(32)  NULL     DEFAULT NULL   COMMENT '班次编码',
    start_time    TIME         NULL     DEFAULT NULL   COMMENT '上班时间',
    end_time      TIME         NULL     DEFAULT NULL   COMMENT '下班时间',
    schedule_type VARCHAR(20)  NOT NULL DEFAULT 'FIXED' COMMENT '排班类型：FIXED/TEMP',
    remark        VARCHAR(255) NULL     DEFAULT NULL   COMMENT '备注',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by     BIGINT       NULL     DEFAULT NULL   COMMENT '创建人ID',
    update_by     BIGINT       NULL     DEFAULT NULL   COMMENT '更新人ID',
    deleted       TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    version       INT          NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
    PRIMARY KEY (id),
    UNIQUE KEY uk_hr_schedule (employee_id, work_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='HR排班';

-- ----------------------------------------------------------------------
-- 3. 打卡记录（hr_checkin）
-- ----------------------------------------------------------------------
CREATE TABLE hr_checkin (
    id            BIGINT        NOT NULL                COMMENT '主键（雪花ID）',
    employee_id   BIGINT        NOT NULL                COMMENT '员工ID（逻辑引用 hr_employee.id）',
    checkin_date  DATE          NOT NULL                COMMENT '打卡日期',
    checkin_time  DATETIME      NULL     DEFAULT NULL   COMMENT '打卡时间',
    checkin_type  VARCHAR(10)   NOT NULL                COMMENT '打卡类型：IN/OUT',
    latitude      DECIMAL(10,7) NULL     DEFAULT NULL   COMMENT '纬度',
    longitude     DECIMAL(10,7) NULL     DEFAULT NULL   COMMENT '经度',
    wifi_ssid     VARCHAR(64)   NULL     DEFAULT NULL   COMMENT 'WiFi名称',
    wifi_bssid    VARCHAR(64)   NULL     DEFAULT NULL   COMMENT 'WiFi MAC地址',
    address       VARCHAR(255)  NULL     DEFAULT NULL   COMMENT '打卡地址',
    source        VARCHAR(20)   NOT NULL DEFAULT 'MINI' COMMENT '来源：MINI/DEVICE/MANUAL',
    status        VARCHAR(20)   NOT NULL DEFAULT 'NORMAL' COMMENT '状态：NORMAL/LATE/EARLY/ABSENT',
    remark        VARCHAR(255)  NULL     DEFAULT NULL   COMMENT '备注',
    create_time   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by     BIGINT        NULL     DEFAULT NULL   COMMENT '创建人ID',
    update_by     BIGINT        NULL     DEFAULT NULL   COMMENT '更新人ID',
    deleted       TINYINT       NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    version       INT           NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
    PRIMARY KEY (id),
    KEY idx_hr_checkin_employee_date (employee_id, checkin_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='HR打卡记录';

-- ----------------------------------------------------------------------
-- 4. 请假申请单（hr_leave_request）
-- ----------------------------------------------------------------------
CREATE TABLE hr_leave_request (
    id              BIGINT       NOT NULL                COMMENT '主键（雪花ID）',
    employee_id     BIGINT       NOT NULL                COMMENT '员工ID（逻辑引用 hr_employee.id）',
    leave_type      VARCHAR(20)  NOT NULL                COMMENT '假期类型：ANNUAL/SICK/PERSONAL',
    start_date      DATE         NOT NULL                COMMENT '请假开始日期',
    end_date        DATE         NOT NULL                COMMENT '请假结束日期',
    days            DECIMAL(6,1) NOT NULL DEFAULT 0.0    COMMENT '请假天数',
    reason          VARCHAR(500) NULL     DEFAULT NULL   COMMENT '请假事由',
    approval_status VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT '审批状态：PENDING/APPROVED/REJECTED/WITHDRAWN/CANCELLED',
    bpm_instance_id BIGINT       NULL     DEFAULT NULL   COMMENT 'BPM流程实例ID（逻辑引用 bpm_instance.id）',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by       BIGINT       NULL     DEFAULT NULL   COMMENT '创建人ID',
    update_by       BIGINT       NULL     DEFAULT NULL   COMMENT '更新人ID',
    deleted         TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    version         INT          NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
    PRIMARY KEY (id),
    KEY idx_hr_leave_request_employee_status (employee_id, approval_status),
    KEY idx_hr_leave_request_instance (bpm_instance_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='HR请假申请单';

-- ----------------------------------------------------------------------
-- 5. 补卡申请单（hr_checkin_supplement）
-- ----------------------------------------------------------------------
CREATE TABLE hr_checkin_supplement (
    id              BIGINT       NOT NULL                COMMENT '主键（雪花ID）',
    employee_id     BIGINT       NOT NULL                COMMENT '员工ID（逻辑引用 hr_employee.id）',
    checkin_date    DATE         NOT NULL                COMMENT '补卡日期',
    checkin_time    DATETIME     NOT NULL                COMMENT '补卡时间',
    checkin_type    VARCHAR(10)  NOT NULL                COMMENT '打卡类型：IN/OUT',
    reason          VARCHAR(500) NULL     DEFAULT NULL   COMMENT '补卡事由',
    approval_status VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT '审批状态：PENDING/APPROVED/REJECTED/WITHDRAWN/CANCELLED',
    bpm_instance_id BIGINT       NULL     DEFAULT NULL   COMMENT 'BPM流程实例ID（逻辑引用 bpm_instance.id）',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by       BIGINT       NULL     DEFAULT NULL   COMMENT '创建人ID',
    update_by       BIGINT       NULL     DEFAULT NULL   COMMENT '更新人ID',
    deleted         TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    version         INT          NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
    PRIMARY KEY (id),
    KEY idx_hr_supplement_employee_date (employee_id, checkin_date),
    KEY idx_hr_supplement_instance (bpm_instance_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='HR补卡申请单';
