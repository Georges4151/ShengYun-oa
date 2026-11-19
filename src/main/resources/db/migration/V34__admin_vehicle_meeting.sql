-- ======================================================================
-- V34：行政后勤 - 车辆与会议室（车辆 / 用车 / 费用 / 会议室 / 预定）
--
-- 说明：
--   1. 主键为雪花 ID（BIGINT），不 AUTO_INCREMENT。
--   2. 用车与会议室均按「时间段重叠」做冲突校验：
--      existing.start_time < new.end_time AND existing.end_time > new.start_time，
--      仅统计 APPROVING/APPROVED（用车含 USING）状态。
--   3. 车辆费用附件存 file 模块返回的服务端文件名，与原计划的 attachment_id 不同。
--   4. 仅增量新增表，不修改任何历史迁移与既有业务表。
-- ======================================================================

-- ----------------------------------------------------------------------
-- 5. 车辆台账（admin_vehicle）
-- ----------------------------------------------------------------------
CREATE TABLE admin_vehicle (
    id                BIGINT       NOT NULL                COMMENT '主键（雪花ID）',
    plate_no          VARCHAR(32)  NOT NULL                COMMENT '车牌号',
    brand_model       VARCHAR(128) NULL     DEFAULT NULL   COMMENT '品牌型号',
    seats             INT          NULL     DEFAULT NULL   COMMENT '座位数',
    purchase_date     DATE         NULL     DEFAULT NULL   COMMENT '购置日期',
    insurance_expire  DATE         NULL     DEFAULT NULL   COMMENT '保险到期日',
    inspection_expire DATE         NULL     DEFAULT NULL   COMMENT '年检到期日',
    status            VARCHAR(20)  NOT NULL DEFAULT 'IDLE' COMMENT '状态：IDLE/USING/MAINTENANCE',
    remark            VARCHAR(500) NULL     DEFAULT NULL   COMMENT '备注',
    create_time       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by         BIGINT       NULL     DEFAULT NULL   COMMENT '创建人ID',
    update_by         BIGINT       NULL     DEFAULT NULL   COMMENT '更新人ID',
    deleted           TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    version           INT          NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
    PRIMARY KEY (id),
    UNIQUE KEY uk_admin_vehicle_plate (plate_no),
    KEY idx_admin_vehicle_status (status),
    KEY idx_admin_vehicle_insurance (insurance_expire),
    KEY idx_admin_vehicle_inspection (inspection_expire)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='行政车辆台账';

-- ----------------------------------------------------------------------
-- 6. 用车申请（admin_vehicle_apply）
-- ----------------------------------------------------------------------
CREATE TABLE admin_vehicle_apply (
    id              BIGINT        NOT NULL                COMMENT '主键（雪花ID）',
    vehicle_id      BIGINT        NOT NULL                COMMENT '车辆ID（逻辑引用 admin_vehicle.id）',
    applicant_id    BIGINT        NOT NULL                COMMENT '申请人ID（sys_user.id）',
    purpose         VARCHAR(255)  NULL     DEFAULT NULL   COMMENT '用车事由',
    destination     VARCHAR(255)  NULL     DEFAULT NULL   COMMENT '目的地',
    start_time      DATETIME      NOT NULL                COMMENT '开始时间',
    end_time        DATETIME      NOT NULL                COMMENT '结束时间',
    bpm_instance_id BIGINT        NULL     DEFAULT NULL   COMMENT 'BPM实例ID',
    status          VARCHAR(20)   NOT NULL DEFAULT 'DRAFT' COMMENT '状态：DRAFT/APPROVING/APPROVED/REJECTED/USING/RETURNED/CANCELLED',
    return_time     DATETIME      NULL     DEFAULT NULL   COMMENT '实际还车时间',
    mileage         DECIMAL(10,1) NULL     DEFAULT NULL   COMMENT '还车里程（公里）',
    create_time     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by       BIGINT        NULL     DEFAULT NULL   COMMENT '创建人ID',
    update_by       BIGINT        NULL     DEFAULT NULL   COMMENT '更新人ID',
    deleted         TINYINT       NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    version         INT           NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
    PRIMARY KEY (id),
    KEY idx_admin_vehicle_apply_vehicle (vehicle_id, status, start_time),
    KEY idx_admin_vehicle_apply_user (applicant_id),
    KEY idx_admin_vehicle_apply_bpm (bpm_instance_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='行政用车申请';

-- ----------------------------------------------------------------------
-- 7. 车辆费用（admin_vehicle_expense）
-- ----------------------------------------------------------------------
CREATE TABLE admin_vehicle_expense (
    id              BIGINT        NOT NULL                COMMENT '主键（雪花ID）',
    vehicle_id      BIGINT        NOT NULL                COMMENT '车辆ID（逻辑引用 admin_vehicle.id）',
    apply_id        BIGINT        NULL     DEFAULT NULL   COMMENT '关联用车单ID（可空）',
    expense_type    VARCHAR(20)   NOT NULL                COMMENT '费用类型：MAINTENANCE/VIOLATION/FUEL',
    amount          DECIMAL(14,2) NOT NULL                COMMENT '金额',
    occur_date      DATE          NOT NULL                COMMENT '发生日期',
    attachment_name VARCHAR(255)  NULL     DEFAULT NULL   COMMENT '附件（file 模块服务端文件名）',
    remark          VARCHAR(500)  NULL     DEFAULT NULL   COMMENT '备注',
    create_time     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by       BIGINT        NULL     DEFAULT NULL   COMMENT '创建人ID',
    update_by       BIGINT        NULL     DEFAULT NULL   COMMENT '更新人ID',
    deleted         TINYINT       NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    version         INT           NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
    PRIMARY KEY (id),
    KEY idx_admin_vehicle_expense_vehicle (vehicle_id, occur_date),
    KEY idx_admin_vehicle_expense_apply (apply_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='行政车辆费用';

-- ----------------------------------------------------------------------
-- 8. 会议室（admin_meeting_room）
-- ----------------------------------------------------------------------
CREATE TABLE admin_meeting_room (
    id          BIGINT       NOT NULL                COMMENT '主键（雪花ID）',
    name        VARCHAR(128) NOT NULL                COMMENT '会议室名称',
    location    VARCHAR(128) NULL     DEFAULT NULL   COMMENT '位置',
    capacity    INT          NULL     DEFAULT NULL   COMMENT '容纳人数',
    equipment   VARCHAR(500) NULL     DEFAULT NULL   COMMENT '设备（逗号分隔）',
    status      VARCHAR(20)  NOT NULL DEFAULT 'ENABLED' COMMENT '状态：ENABLED/DISABLED',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by   BIGINT       NULL     DEFAULT NULL   COMMENT '创建人ID',
    update_by   BIGINT       NULL     DEFAULT NULL   COMMENT '更新人ID',
    deleted     TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    version     INT          NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
    PRIMARY KEY (id),
    UNIQUE KEY uk_admin_meeting_room_name (name),
    KEY idx_admin_meeting_room_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='行政会议室';

-- ----------------------------------------------------------------------
-- 9. 会议室预定（admin_meeting_apply）
-- ----------------------------------------------------------------------
CREATE TABLE admin_meeting_apply (
    id              BIGINT        NOT NULL                COMMENT '主键（雪花ID）',
    room_id         BIGINT        NOT NULL                COMMENT '会议室ID（逻辑引用 admin_meeting_room.id）',
    applicant_id    BIGINT        NOT NULL                COMMENT '申请人ID（sys_user.id）',
    subject         VARCHAR(255)  NOT NULL                COMMENT '会议主题',
    start_time      DATETIME      NOT NULL                COMMENT '开始时间',
    end_time        DATETIME      NOT NULL                COMMENT '结束时间',
    attendee_ids    VARCHAR(1000) NULL     DEFAULT NULL   COMMENT '参会人ID，逗号分隔',
    bpm_instance_id BIGINT        NULL     DEFAULT NULL   COMMENT 'BPM实例ID',
    status          VARCHAR(20)   NOT NULL DEFAULT 'DRAFT' COMMENT '状态：DRAFT/APPROVING/APPROVED/REJECTED/CANCELLED',
    create_time     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by       BIGINT        NULL     DEFAULT NULL   COMMENT '创建人ID',
    update_by       BIGINT        NULL     DEFAULT NULL   COMMENT '更新人ID',
    deleted         TINYINT       NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    version         INT           NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
    PRIMARY KEY (id),
    KEY idx_admin_meeting_apply_room (room_id, start_time, end_time),
    KEY idx_admin_meeting_apply_user (applicant_id),
    KEY idx_admin_meeting_apply_bpm (bpm_instance_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='行政会议室预定';
