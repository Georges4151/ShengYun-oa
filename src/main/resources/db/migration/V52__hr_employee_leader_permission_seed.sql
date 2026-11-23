-- ======================================================================
-- V52：员工上下级维护权限点（幂等）
--
-- 说明：
--   1. ID 续接 V49（sys_permission 最大 195，sys_role_permission 已用至 1137）：
--      本脚本 sys_permission 使用 196，sys_role_permission 使用 1138。
--   2. 按钮权限 hr:employee:leader 挂在 HR 人事菜单 157（code=hr）下，
--      与 hr:employee:edit（159，维护员工档案）并列，用于独立控制「直属上级」的指定与调整；
--      不授予该权限点时，员工表单中的「直属上级」只读。
--   3. 仅授予 SYS_ADMIN（role_id=1）；其他角色可由管理员在「账号权限」中单独勾选。
--   4. 所有语句使用 INSERT ... SELECT ... WHERE NOT EXISTS，可重复执行；
--      权限点按 code 判重，角色-权限按 (role_id, permission_id) 判重。
--   5. 本脚本为增量迁移，入库后不得修改；权限点变更另起更高版本号脚本。
-- ======================================================================

-- ----------------------------------------------------------------------
-- 一、权限点（sys_permission）
-- ----------------------------------------------------------------------
INSERT INTO sys_permission (id, code, name, type, parent_id, path, icon, sort_order, deleted, version)
SELECT 196, 'hr:employee:leader', '维护员工上下级', 'BUTTON', 157, NULL, NULL, 64, 0, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'hr:employee:leader');

-- ----------------------------------------------------------------------
-- 二、角色-权限矩阵（sys_role_permission，仅 SYS_ADMIN）
-- ----------------------------------------------------------------------
INSERT INTO sys_role_permission (id, role_id, permission_id)
SELECT 1138, 1, 196
WHERE NOT EXISTS (SELECT 1 FROM sys_role_permission WHERE role_id = 1 AND permission_id = 196);
