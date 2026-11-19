-- ======================================================================
-- V49：发布通知按钮权限点（幂等）
--
-- 说明：
--   1. ID 续接 V48（V48 已占用 sys_permission 192～194、sys_role_permission 1130～1135）：
--      本脚本 sys_permission 使用 195，sys_role_permission 使用 1136、1137。
--   2. 按钮权限 notification:publish 挂在「通知」菜单 133（code=notification，
--      type=MENU，path=/notification）下，与已有 notification:send（135）并列；
--      「发送」与「发布」为相互独立的操作权限点，互不包含。
--   3. 所有语句使用 INSERT ... SELECT ... WHERE NOT EXISTS，可重复执行：
--      权限点按 code 判重，角色-权限按 (role_id, permission_id) 判重。
--   4. 仅授予 SYS_ADMIN（role_id=1）与 FINANCE（role_id=2）；
--      其他角色可由管理员在「账号权限」中单独勾选。
--   5. 本脚本为增量迁移，入库后不得修改；权限点变更另起更高版本号脚本。
-- ======================================================================

-- ----------------------------------------------------------------------
-- 一、权限点（sys_permission）
-- ----------------------------------------------------------------------
INSERT INTO sys_permission (id, code, name, type, parent_id, path, icon, sort_order, deleted, version)
SELECT 195, 'notification:publish', '发布通知', 'BUTTON', 133, NULL, NULL, 38, 0, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'notification:publish');

-- ----------------------------------------------------------------------
-- 二、角色-权限矩阵（sys_role_permission，共 2 条）
--     角色：1=SYS_ADMIN，2=FINANCE
-- ----------------------------------------------------------------------
INSERT INTO sys_role_permission (id, role_id, permission_id)
SELECT 1136, 1, 195
WHERE NOT EXISTS (SELECT 1 FROM sys_role_permission WHERE role_id = 1 AND permission_id = 195);

INSERT INTO sys_role_permission (id, role_id, permission_id)
SELECT 1137, 2, 195
WHERE NOT EXISTS (SELECT 1 FROM sys_role_permission WHERE role_id = 2 AND permission_id = 195);
