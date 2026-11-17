-- ======================================================================
-- V30：HR 薪资绩效权限点种子（幂等）
--
-- 说明：
--   1. ID 续接 V25：sys_permission 权限点从 164 起（M1 已占用至 163），
--      sys_role_permission 从 1096 起（已占用至 1095）。
--   2. 所有语句均使用 INSERT ... SELECT ... WHERE NOT EXISTS，脚本可重复执行：
--      权限点按 code 判重，角色-权限按 (role_id, permission_id) 判重。
--   3. 仅给 SYS_ADMIN（role_id=1）与 FINANCE（role_id=2）授权，不新建角色。
--   4. 子权限全部挂在 HR 菜单（parent_id=157）下，保证前端菜单可见。
--   5. 本人查看工资条类接口不设权限点，仅要求登录。
-- ======================================================================

-- ----------------------------------------------------------------------
-- 一、权限点（sys_permission）
-- ----------------------------------------------------------------------
INSERT INTO sys_permission (id, code, name, type, parent_id, path, icon, sort_order, deleted, version)
SELECT 164, 'hr:salary:view', '查看薪资', 'API', 157, NULL, NULL, 58, 0, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'hr:salary:view');

INSERT INTO sys_permission (id, code, name, type, parent_id, path, icon, sort_order, deleted, version)
SELECT 165, 'hr:salary:change', '发起薪资变动', 'BUTTON', 157, NULL, NULL, 59, 0, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'hr:salary:change');

INSERT INTO sys_permission (id, code, name, type, parent_id, path, icon, sort_order, deleted, version)
SELECT 166, 'hr:salary:confirm', '确认薪资核算', 'BUTTON', 157, NULL, NULL, 60, 0, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'hr:salary:confirm');

INSERT INTO sys_permission (id, code, name, type, parent_id, path, icon, sort_order, deleted, version)
SELECT 167, 'hr:performance:template', '管理绩效模板', 'BUTTON', 157, NULL, NULL, 61, 0, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'hr:performance:template');

INSERT INTO sys_permission (id, code, name, type, parent_id, path, icon, sort_order, deleted, version)
SELECT 168, 'hr:performance:operate', '操作绩效考评', 'BUTTON', 157, NULL, NULL, 62, 0, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'hr:performance:operate');

INSERT INTO sys_permission (id, code, name, type, parent_id, path, icon, sort_order, deleted, version)
SELECT 169, 'hr:salary-slip:export', '导出工资条', 'BUTTON', 157, NULL, NULL, 63, 0, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'hr:salary-slip:export');

-- ----------------------------------------------------------------------
-- 二、角色-权限矩阵（sys_role_permission）
--     SYS_ADMIN 全量；FINANCE 仅授予薪资查看（164）。
-- ----------------------------------------------------------------------
INSERT INTO sys_role_permission (id, role_id, permission_id)
SELECT 1096, 1, 164
WHERE NOT EXISTS (SELECT 1 FROM sys_role_permission WHERE role_id = 1 AND permission_id = 164);

INSERT INTO sys_role_permission (id, role_id, permission_id)
SELECT 1097, 1, 165
WHERE NOT EXISTS (SELECT 1 FROM sys_role_permission WHERE role_id = 1 AND permission_id = 165);

INSERT INTO sys_role_permission (id, role_id, permission_id)
SELECT 1098, 1, 166
WHERE NOT EXISTS (SELECT 1 FROM sys_role_permission WHERE role_id = 1 AND permission_id = 166);

INSERT INTO sys_role_permission (id, role_id, permission_id)
SELECT 1099, 1, 167
WHERE NOT EXISTS (SELECT 1 FROM sys_role_permission WHERE role_id = 1 AND permission_id = 167);

INSERT INTO sys_role_permission (id, role_id, permission_id)
SELECT 1100, 1, 168
WHERE NOT EXISTS (SELECT 1 FROM sys_role_permission WHERE role_id = 1 AND permission_id = 168);

INSERT INTO sys_role_permission (id, role_id, permission_id)
SELECT 1101, 1, 169
WHERE NOT EXISTS (SELECT 1 FROM sys_role_permission WHERE role_id = 1 AND permission_id = 169);

INSERT INTO sys_role_permission (id, role_id, permission_id)
SELECT 1102, 2, 164
WHERE NOT EXISTS (SELECT 1 FROM sys_role_permission WHERE role_id = 2 AND permission_id = 164);
