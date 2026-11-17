-- ======================================================================
-- V42：M6 报表权限点种子（幂等）
--
-- 说明：
--   1. ID 续接 V41：sys_permission 从 186 起、sys_role_permission 从 1124 起。
--   2. 所有语句均使用 INSERT ... SELECT ... WHERE NOT EXISTS，脚本可重复执行：
--      权限点按 code 判重，角色-权限按 (role_id, permission_id) 判重。
--   3. 菜单 186 为「报表」，按钮 187～189 挂其下，仅授予 SYS_ADMIN（role_id=1）；
--      报表涉及成本与绩效等敏感数据，不授予 FINANCE / STAFF。
--   4. 本脚本为增量迁移，入库后不得修改；权限点变更另起更高版本号脚本。
-- ======================================================================

-- ----------------------------------------------------------------------
-- 一、权限点（sys_permission）
-- ----------------------------------------------------------------------
INSERT INTO sys_permission (id, code, name, type, parent_id, path, icon, sort_order, deleted, version)
SELECT 186, 'report', '报表', 'MENU', NULL, '/report', 'DataLine', 80, 0, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'report');

INSERT INTO sys_permission (id, code, name, type, parent_id, path, icon, sort_order, deleted, version)
SELECT 187, 'report:hr-cost:view', '人力成本报表', 'BUTTON', 186, NULL, NULL, 81, 0, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'report:hr-cost:view');

INSERT INTO sys_permission (id, code, name, type, parent_id, path, icon, sort_order, deleted, version)
SELECT 188, 'report:hr-performance:view', '绩效分布报表', 'BUTTON', 186, NULL, NULL, 82, 0, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'report:hr-performance:view');

INSERT INTO sys_permission (id, code, name, type, parent_id, path, icon, sort_order, deleted, version)
SELECT 189, 'report:admin-assets:view', '资产报表', 'BUTTON', 186, NULL, NULL, 83, 0, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'report:admin-assets:view');

-- ----------------------------------------------------------------------
-- 二、角色-权限矩阵（sys_role_permission，共 4 条）
--     角色：1=SYS_ADMIN
-- ----------------------------------------------------------------------
INSERT INTO sys_role_permission (id, role_id, permission_id)
SELECT 1124, 1, 186
WHERE NOT EXISTS (SELECT 1 FROM sys_role_permission WHERE role_id = 1 AND permission_id = 186);

INSERT INTO sys_role_permission (id, role_id, permission_id)
SELECT 1125, 1, 187
WHERE NOT EXISTS (SELECT 1 FROM sys_role_permission WHERE role_id = 1 AND permission_id = 187);

INSERT INTO sys_role_permission (id, role_id, permission_id)
SELECT 1126, 1, 188
WHERE NOT EXISTS (SELECT 1 FROM sys_role_permission WHERE role_id = 1 AND permission_id = 188);

INSERT INTO sys_role_permission (id, role_id, permission_id)
SELECT 1127, 1, 189
WHERE NOT EXISTS (SELECT 1 FROM sys_role_permission WHERE role_id = 1 AND permission_id = 189);
