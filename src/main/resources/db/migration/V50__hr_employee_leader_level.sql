-- ======================================================================
-- V50：HR 组织层级与职级字典（职级表 / 员工直属上级 / 晋升目标职级）
--
-- 说明：
--   1. 新建职级字典表 hr_job_level，表结构写法与 V23 的 hr_position 保持一致。
--   2. hr_employee 增加 leader_id（直属上级员工）与 level_id（职级）两列，
--      均为逻辑引用，不建物理外键。
--   3. hr_employee_change 增加 target_level_id，供 PROMOTION（晋升）申请使用。
--   4. 按 sys_user.leader_id 回填存量员工的直属上级，只回填上级员工存在的记录；
--      无法回填的清单查询见文件末尾注释。
--   5. sys_user.leader_id 自本版本起停止读写，列保留以兼容历史数据。
-- ======================================================================

-- ----------------------------------------------------------------------
-- 1. 职级字典（hr_job_level）
-- ----------------------------------------------------------------------
CREATE TABLE hr_job_level (
    id          BIGINT       NOT NULL                COMMENT '主键（雪花ID）',
    code        VARCHAR(64)  NOT NULL                COMMENT '职级编码（唯一）',
    name        VARCHAR(128) NOT NULL                COMMENT '职级名称',
    job_family  VARCHAR(64)  NULL     DEFAULT NULL   COMMENT '职级序列（技术/管理/职能等）',
    rank_no     INT          NULL     DEFAULT NULL   COMMENT '职级序号（数值越大职级越高）',
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
    UNIQUE KEY uk_hr_job_level_code (code),
    KEY idx_hr_job_level_status (status, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='HR职级字典';

-- ----------------------------------------------------------------------
-- 2. 员工档案增加直属上级与职级
-- ----------------------------------------------------------------------
ALTER TABLE hr_employee
    ADD COLUMN leader_id BIGINT NULL DEFAULT NULL COMMENT '直属上级员工ID（逻辑引用 hr_employee.id）',
    ADD COLUMN level_id  BIGINT NULL DEFAULT NULL COMMENT '职级ID（逻辑引用 hr_job_level.id）',
    ADD KEY idx_hr_employee_leader (leader_id),
    ADD KEY idx_hr_employee_level (level_id);

-- ----------------------------------------------------------------------
-- 3. 入转调离申请单增加晋升目标职级
-- ----------------------------------------------------------------------
ALTER TABLE hr_employee_change
    ADD COLUMN target_level_id BIGINT NULL DEFAULT NULL COMMENT '晋升目标职级ID（PROMOTION 使用）';

-- ----------------------------------------------------------------------
-- 4. 按 sys_user.leader_id 回填直属上级员工（只回填上级员工存在的记录）
-- ----------------------------------------------------------------------
UPDATE hr_employee e
    JOIN sys_user u ON u.id = e.user_id AND u.deleted = 0
    JOIN hr_employee l ON l.user_id = u.leader_id AND l.deleted = 0 AND l.id <> e.id
SET e.leader_id = l.id
WHERE e.leader_id IS NULL AND u.leader_id IS NOT NULL AND e.deleted = 0;

-- ----------------------------------------------------------------------
-- 5. sys_user.leader_id 废弃（列保留，停止读写，由 hr_employee.leader_id 承载）
-- ----------------------------------------------------------------------
ALTER TABLE sys_user
    MODIFY COLUMN leader_id BIGINT NULL DEFAULT NULL COMMENT '已废弃：V50 起由 hr_employee.leader_id 承载，本列停止读写';

-- ----------------------------------------------------------------------
-- 未回填清单（人工核对用，不参与迁移执行）。按处理方式分为四类：
-- ----------------------------------------------------------------------
-- A. 真正回填失败：系统账号有上级，但上级用户未绑定员工档案，需人工补录
-- SELECT e.employee_no, e.name AS employee_name,
--        u.id AS user_id, u.name AS user_name,
--        u.leader_id AS sys_leader_user_id,
--        lu.name AS sys_leader_user_name
-- FROM hr_employee e
--          JOIN sys_user u ON u.id = e.user_id AND u.deleted = 0
--          LEFT JOIN hr_employee l ON l.user_id = u.leader_id AND l.deleted = 0
--          LEFT JOIN sys_user lu ON lu.id = u.leader_id
-- WHERE e.deleted = 0
--   AND e.leader_id IS NULL
--   AND u.leader_id IS NOT NULL
--   AND l.id IS NULL;
--
-- B. 员工未绑定系统账号：无法继承上级，需先绑定账号
-- SELECT e.employee_no, e.name AS employee_name, e.status, e.dept_id
-- FROM hr_employee e
-- WHERE e.deleted = 0 AND e.user_id IS NULL;
--
-- C. 本来就没有上级：正常数据，无需处理
-- SELECT e.employee_no, e.name AS employee_name
-- FROM hr_employee e
--          JOIN sys_user u ON u.id = e.user_id AND u.deleted = 0
-- WHERE e.deleted = 0
--   AND e.leader_id IS NULL
--   AND u.leader_id IS NULL;
--
-- D. 回填后脏数据：自己成为自己的上级（应为空）
-- SELECT id, employee_no, name FROM hr_employee
-- WHERE deleted = 0 AND leader_id = id;
