-- ======================================================================
-- V25：HR 人事权限点种子（幂等）
--
-- 说明：
--   1. 沿用 V11/V16 的显式 ID 风格：权限点从 157 起（150/151 已被 V20 的
--      iam:dept:view / iam:dept:manage 占用），
--      sys_role_permission 从 1089 起（现有最大 1088）。
--   2. 所有语句均使用 INSERT ... SELECT ... WHERE NOT EXISTS，脚本可重复执行。
--   3. 仅给 SYS_ADMIN（role_id=1）授权，不新建角色。
--   4. 授予子权限时一并授予父 MENU（157），保证前端菜单可见。
-- ======================================================================

-- ----------------------------------------------------------------------
-- 一、权限点（sys_permission）
-- ----------------------------------------------------------------------
INSERT INTO sys_permission (id, code, name, type, parent_id, path, icon, sort_order, deleted, version)
SELECT 157, 'hr', 'HR人事', 'MENU', NULL, '/hr', 'User', 51, 0, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'hr');

INSERT INTO sys_permission (id, code, name, type, parent_id, path, icon, sort_order, deleted, version)
SELECT 158, 'hr:employee:view', '查看员工档案', 'API', 157, NULL, NULL, 52, 0, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'hr:employee:view');

INSERT INTO sys_permission (id, code, name, type, parent_id, path, icon, sort_order, deleted, version)
SELECT 159, 'hr:employee:edit', '维护员工档案', 'BUTTON', 157, NULL, NULL, 53, 0, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'hr:employee:edit');

INSERT INTO sys_permission (id, code, name, type, parent_id, path, icon, sort_order, deleted, version)
SELECT 160, 'hr:contract:manage', '管理劳动合同', 'BUTTON', 157, NULL, NULL, 54, 0, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'hr:contract:manage');

INSERT INTO sys_permission (id, code, name, type, parent_id, path, icon, sort_order, deleted, version)
SELECT 161, 'hr:schedule:manage', '管理排班', 'BUTTON', 157, NULL, NULL, 55, 0, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'hr:schedule:manage');

INSERT INTO sys_permission (id, code, name, type, parent_id, path, icon, sort_order, deleted, version)
SELECT 162, 'hr:attendance:view', '查看考勤', 'API', 157, NULL, NULL, 56, 0, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'hr:attendance:view');

INSERT INTO sys_permission (id, code, name, type, parent_id, path, icon, sort_order, deleted, version)
SELECT 163, 'hr:leave:manage', '管理请假额度', 'BUTTON', 157, NULL, NULL, 57, 0, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'hr:leave:manage');

-- ----------------------------------------------------------------------
-- 二、角色-权限矩阵（sys_role_permission，仅 SYS_ADMIN）
-- ----------------------------------------------------------------------
INSERT INTO sys_role_permission (id, role_id, permission_id)
SELECT 1089, 1, 157
WHERE NOT EXISTS (SELECT 1 FROM sys_role_permission WHERE role_id = 1 AND permission_id = 157);

INSERT INTO sys_role_permission (id, role_id, permission_id)
SELECT 1090, 1, 158
WHERE NOT EXISTS (SELECT 1 FROM sys_role_permission WHERE role_id = 1 AND permission_id = 158);

INSERT INTO sys_role_permission (id, role_id, permission_id)
SELECT 1091, 1, 159
WHERE NOT EXISTS (SELECT 1 FROM sys_role_permission WHERE role_id = 1 AND permission_id = 159);

INSERT INTO sys_role_permission (id, role_id, permission_id)
SELECT 1092, 1, 160
WHERE NOT EXISTS (SELECT 1 FROM sys_role_permission WHERE role_id = 1 AND permission_id = 160);

INSERT INTO sys_role_permission (id, role_id, permission_id)
SELECT 1093, 1, 161
WHERE NOT EXISTS (SELECT 1 FROM sys_role_permission WHERE role_id = 1 AND permission_id = 161);

INSERT INTO sys_role_permission (id, role_id, permission_id)
SELECT 1094, 1, 162
WHERE NOT EXISTS (SELECT 1 FROM sys_role_permission WHERE role_id = 1 AND permission_id = 162);

INSERT INTO sys_role_permission (id, role_id, permission_id)
SELECT 1095, 1, 163
WHERE NOT EXISTS (SELECT 1 FROM sys_role_permission WHERE role_id = 1 AND permission_id = 163);
