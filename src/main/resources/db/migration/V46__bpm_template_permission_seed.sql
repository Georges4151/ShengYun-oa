-- ======================================================================
-- V46：流程模板「新增 / 删除」按钮权限点（幂等）
--
-- 说明：
--   1. ID 续接 V42：sys_permission 从 190 起、sys_role_permission 从 1128 起。
--   2. 两个按钮权限挂在 BPM 菜单 143 下，与已有 bpm:template:manage 并列；
--      发布版本、启用/停用仍由 bpm:template:manage 控制。
--   3. 仅授予 SYS_ADMIN（role_id=1），管理员默认拥有全部权限；
--      其他角色可由管理员在「账号权限」中单独勾选。
--   4. 所有语句使用 INSERT ... SELECT ... WHERE NOT EXISTS，可重复执行。
-- ======================================================================

-- ----------------------------------------------------------------------
-- 一、权限点（sys_permission）
-- ----------------------------------------------------------------------
INSERT INTO sys_permission (id, code, name, type, parent_id, path, icon, sort_order, deleted, version)
SELECT 190, 'bpm:template:create', '新增流程模板', 'BUTTON', 143, NULL, NULL, 51, 0, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'bpm:template:create');

INSERT INTO sys_permission (id, code, name, type, parent_id, path, icon, sort_order, deleted, version)
SELECT 191, 'bpm:template:delete', '删除流程模板', 'BUTTON', 143, NULL, NULL, 52, 0, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'bpm:template:delete');

-- ----------------------------------------------------------------------
-- 二、角色-权限矩阵（sys_role_permission）
--     角色：1=SYS_ADMIN
-- ----------------------------------------------------------------------
INSERT INTO sys_role_permission (id, role_id, permission_id)
SELECT 1128, 1, 190
WHERE NOT EXISTS (SELECT 1 FROM sys_role_permission WHERE role_id = 1 AND permission_id = 190);

INSERT INTO sys_role_permission (id, role_id, permission_id)
SELECT 1129, 1, 191
WHERE NOT EXISTS (SELECT 1 FROM sys_role_permission WHERE role_id = 1 AND permission_id = 191);
