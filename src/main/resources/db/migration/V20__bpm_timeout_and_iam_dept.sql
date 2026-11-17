-- ======================================================================
-- V20：BPM 超时提醒落库 + IAM 部门种子与权限点
--
-- 说明：
--   1. bpm_task 增加 reminded_at：记录超时提醒的抢占时间，NULL 表示尚未提醒。
--      定时任务通过「条件更新 reminded_at IS NULL -> 当前时间」抢占提醒权，
--      保证并发与多实例部署下同一任务只提醒一次。
--   2. 新增索引 (status, due_at, reminded_at)：与超时扫描条件
--      (status = 'PENDING' AND due_at < now AND reminded_at IS NULL) 对齐，
--      V14 已建 (status, due_at)，本迁移补齐 reminded_at 列以覆盖该查询。
--   3. 组织种子：幂等插入根部门「总公司」（已存在则跳过）。
--   4. 权限点：沿用 V11/V16 的显式 ID 风格，权限点从 150 起；SYS_ADMIN 关联
--      从 1082 起，均避开已有 ID，且全部使用 INSERT ... SELECT ... WHERE NOT EXISTS
--      保证脚本可重复执行（Flyway 重复执行或人工重放均安全）。
--   5. 本迁移不修改任何电子印章相关表与逻辑。
-- ======================================================================

-- ----------------------------------------------------------------------
-- 一、bpm_task.reminded_at（幂等加列）
-- ----------------------------------------------------------------------
SET @reminded_col_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'bpm_task'
      AND COLUMN_NAME = 'reminded_at'
);

SET @reminded_col_ddl = IF(
    @reminded_col_exists = 0,
    'ALTER TABLE bpm_task ADD COLUMN reminded_at DATETIME NULL DEFAULT NULL COMMENT ''超时提醒时间（NULL 表示尚未提醒）'' AFTER due_at',
    'SELECT 1'
);

PREPARE reminded_col_stmt FROM @reminded_col_ddl;
EXECUTE reminded_col_stmt;
DEALLOCATE PREPARE reminded_col_stmt;

-- ----------------------------------------------------------------------
-- 二、超时扫描索引（幂等加索引）
-- ----------------------------------------------------------------------
SET @reminded_idx_exists = (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'bpm_task'
      AND INDEX_NAME = 'idx_bpm_task_status_due_reminded'
);

SET @reminded_idx_ddl = IF(
    @reminded_idx_exists = 0,
    'ALTER TABLE bpm_task ADD KEY idx_bpm_task_status_due_reminded (status, due_at, reminded_at)',
    'SELECT 1'
);

PREPARE reminded_idx_stmt FROM @reminded_idx_ddl;
EXECUTE reminded_idx_stmt;
DEALLOCATE PREPARE reminded_idx_stmt;

-- ----------------------------------------------------------------------
-- 三、组织种子：根部门「总公司」（幂等）
-- ----------------------------------------------------------------------
INSERT INTO sys_dept (id, name, parent_id, leader_id, sort_order, deleted, version)
SELECT
    COALESCE((SELECT MAX(id) FROM sys_dept), 0) + 1,
    '总公司',
    NULL,
    NULL,
    0,
    0,
    0
WHERE NOT EXISTS (
    SELECT 1 FROM sys_dept WHERE name = '总公司' AND parent_id IS NULL AND deleted = 0
);

-- ----------------------------------------------------------------------
-- 四、权限点（sys_permission，幂等）
-- ----------------------------------------------------------------------
INSERT INTO sys_permission (id, code, name, type, parent_id, path, icon, sort_order, deleted, version)
SELECT 150, 'iam:dept:view', '查看部门', 'API', 137, NULL, NULL, 51, 0, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'iam:dept:view');

INSERT INTO sys_permission (id, code, name, type, parent_id, path, icon, sort_order, deleted, version)
SELECT 151, 'iam:dept:manage', '管理部门', 'BUTTON', 137, NULL, NULL, 52, 0, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'iam:dept:manage');

-- ----------------------------------------------------------------------
-- 五、角色-权限矩阵：SYS_ADMIN（role_id = 1，幂等）
-- ----------------------------------------------------------------------
INSERT INTO sys_role_permission (id, role_id, permission_id)
SELECT 1082, 1, 150
WHERE NOT EXISTS (
    SELECT 1 FROM sys_role_permission WHERE role_id = 1 AND permission_id = 150
);

INSERT INTO sys_role_permission (id, role_id, permission_id)
SELECT 1083, 1, 151
WHERE NOT EXISTS (
    SELECT 1 FROM sys_role_permission WHERE role_id = 1 AND permission_id = 151
);
