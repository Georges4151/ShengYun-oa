-- ======================================================================
-- V41：企业微信集成权限点种子（幂等）
--
-- 说明：
--   1. ID 续接 V39：sys_permission 从 184 起（V39 已占用至 183），
--      sys_role_permission 从 1122 起（V39 已占用至 1121）。
--   2. 所有语句均使用 INSERT ... SELECT ... WHERE NOT EXISTS，脚本可重复执行：
--      权限点按 code 判重，角色-权限按 (role_id, permission_id) 判重。
--   3. 菜单 184 为「企业微信集成」，按钮 185 挂其下，仅授予 SYS_ADMIN（role_id=1）；
--      同步与重投为高风险操作，不授予 FINANCE/STAFF。
--   4. 本脚本为增量迁移，入库后不得修改；权限点变更另起更高版本号脚本。
-- ======================================================================

-- ----------------------------------------------------------------------
-- 一、权限点（sys_permission）
-- ----------------------------------------------------------------------
INSERT INTO sys_permission (id, code, name, type, parent_id, path, icon, sort_order, deleted, version)
SELECT 184, 'integration:wecom:view', '企业微信集成', 'MENU', NULL, '/integration/wecom', 'ChatDotRound', 78, 0, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'integration:wecom:view');

INSERT INTO sys_permission (id, code, name, type, parent_id, path, icon, sort_order, deleted, version)
SELECT 185, 'integration:wecom:sync', '手动同步/重投', 'BUTTON', 184, NULL, NULL, 79, 0, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'integration:wecom:sync');

-- ----------------------------------------------------------------------
-- 二、角色-权限矩阵（sys_role_permission，共 2 条）
--     角色：1=SYS_ADMIN
-- ----------------------------------------------------------------------
INSERT INTO sys_role_permission (id, role_id, permission_id)
SELECT 1122, 1, 184
WHERE NOT EXISTS (SELECT 1 FROM sys_role_permission WHERE role_id = 1 AND permission_id = 184);

INSERT INTO sys_role_permission (id, role_id, permission_id)
SELECT 1123, 1, 185
WHERE NOT EXISTS (SELECT 1 FROM sys_role_permission WHERE role_id = 1 AND permission_id = 185);
