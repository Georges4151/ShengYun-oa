-- ======================================================================
-- V36：行政后勤权限点种子（幂等）
--
-- 说明：
--   1. ID 续接 V30：sys_permission 从 170 起（M2 已占用至 169），
--      sys_role_permission 从 1103 起（已占用至 1102）。
--   2. 所有语句均使用 INSERT ... SELECT ... WHERE NOT EXISTS，脚本可重复执行：
--      权限点按 code 判重，角色-权限按 (role_id, permission_id) 判重。
--   3. 仅给 SYS_ADMIN（role_id=1）授权；STAFF 不授菜单，走小程序自助入口。
--   4. 子权限全部挂在「行政后勤」菜单（parent_id=170）下，保证前端菜单可见。
--   5. 本人申请类接口不设权限点，仅要求登录。
-- ======================================================================

-- ----------------------------------------------------------------------
-- 一、权限点（sys_permission）
-- ----------------------------------------------------------------------
INSERT INTO sys_permission (id, code, name, type, parent_id, path, icon, sort_order, deleted, version)
SELECT 170, 'admin', '行政后勤', 'MENU', NULL, '/admin', 'Box', 64, 0, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'admin');

INSERT INTO sys_permission (id, code, name, type, parent_id, path, icon, sort_order, deleted, version)
SELECT 171, 'admin:asset:view', '查看资产', 'API', 170, NULL, NULL, 65, 0, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'admin:asset:view');

INSERT INTO sys_permission (id, code, name, type, parent_id, path, icon, sort_order, deleted, version)
SELECT 172, 'admin:asset:manage', '管理资产', 'BUTTON', 170, NULL, NULL, 66, 0, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'admin:asset:manage');

INSERT INTO sys_permission (id, code, name, type, parent_id, path, icon, sort_order, deleted, version)
SELECT 173, 'admin:material:manage', '管理易耗品', 'BUTTON', 170, NULL, NULL, 67, 0, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'admin:material:manage');

INSERT INTO sys_permission (id, code, name, type, parent_id, path, icon, sort_order, deleted, version)
SELECT 174, 'admin:vehicle:manage', '管理车辆', 'BUTTON', 170, NULL, NULL, 68, 0, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'admin:vehicle:manage');

INSERT INTO sys_permission (id, code, name, type, parent_id, path, icon, sort_order, deleted, version)
SELECT 175, 'admin:meeting:manage', '管理会议室', 'BUTTON', 170, NULL, NULL, 69, 0, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'admin:meeting:manage');

INSERT INTO sys_permission (id, code, name, type, parent_id, path, icon, sort_order, deleted, version)
SELECT 176, 'admin:visitor:manage', '管理访客', 'BUTTON', 170, NULL, NULL, 70, 0, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'admin:visitor:manage');

INSERT INTO sys_permission (id, code, name, type, parent_id, path, icon, sort_order, deleted, version)
SELECT 177, 'admin:repair:manage', '管理报修', 'BUTTON', 170, NULL, NULL, 71, 0, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'admin:repair:manage');

-- ----------------------------------------------------------------------
-- 二、角色-权限矩阵（sys_role_permission，仅 SYS_ADMIN）
-- ----------------------------------------------------------------------
INSERT INTO sys_role_permission (id, role_id, permission_id)
SELECT 1103, 1, 170
WHERE NOT EXISTS (SELECT 1 FROM sys_role_permission WHERE role_id = 1 AND permission_id = 170);

INSERT INTO sys_role_permission (id, role_id, permission_id)
SELECT 1104, 1, 171
WHERE NOT EXISTS (SELECT 1 FROM sys_role_permission WHERE role_id = 1 AND permission_id = 171);

INSERT INTO sys_role_permission (id, role_id, permission_id)
SELECT 1105, 1, 172
WHERE NOT EXISTS (SELECT 1 FROM sys_role_permission WHERE role_id = 1 AND permission_id = 172);

INSERT INTO sys_role_permission (id, role_id, permission_id)
SELECT 1106, 1, 173
WHERE NOT EXISTS (SELECT 1 FROM sys_role_permission WHERE role_id = 1 AND permission_id = 173);

INSERT INTO sys_role_permission (id, role_id, permission_id)
SELECT 1107, 1, 174
WHERE NOT EXISTS (SELECT 1 FROM sys_role_permission WHERE role_id = 1 AND permission_id = 174);

INSERT INTO sys_role_permission (id, role_id, permission_id)
SELECT 1108, 1, 175
WHERE NOT EXISTS (SELECT 1 FROM sys_role_permission WHERE role_id = 1 AND permission_id = 175);

INSERT INTO sys_role_permission (id, role_id, permission_id)
SELECT 1109, 1, 176
WHERE NOT EXISTS (SELECT 1 FROM sys_role_permission WHERE role_id = 1 AND permission_id = 176);

INSERT INTO sys_role_permission (id, role_id, permission_id)
SELECT 1110, 1, 177
WHERE NOT EXISTS (SELECT 1 FROM sys_role_permission WHERE role_id = 1 AND permission_id = 177);
