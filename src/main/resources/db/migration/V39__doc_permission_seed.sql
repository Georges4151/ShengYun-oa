-- ======================================================================
-- V39：文档知识库权限点种子（幂等）
--
-- 说明：
--   1. ID 续接 V36：sys_permission 从 178 起（V36 已占用至 177），
--      sys_role_permission 从 1111 起（V36 已占用至 1110）。
--   2. 文档 7.7 仅给出区间起点 1111～1116，实际授权为 11 条
--      （178×3 + 179×1 + 180×3 + 181×2 + 182×1 + 183×1），
--      故按实际条数连续编号 1111～1121。
--   3. 所有语句均使用 INSERT ... SELECT ... WHERE NOT EXISTS，脚本可重复执行：
--      权限点按 code 判重，角色-权限按 (role_id, permission_id) 判重。
--   4. 子权限全部挂在「文档知识库」菜单（parent_id=178）下，类型为 BUTTON。
--   5. 下载/预览不设独立权限点，由登录 + 数据权限（DocAccessResolver）判定。
--   6. 本脚本为增量迁移，入库后不得修改；权限点变更另起更高版本号脚本。
-- ======================================================================

-- ----------------------------------------------------------------------
-- 一、权限点（sys_permission）
-- ----------------------------------------------------------------------
INSERT INTO sys_permission (id, code, name, type, parent_id, path, icon, sort_order, deleted, version)
SELECT 178, 'doc', '文档知识库', 'MENU', NULL, '/doc', 'Folder', 72, 0, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'doc');

INSERT INTO sys_permission (id, code, name, type, parent_id, path, icon, sort_order, deleted, version)
SELECT 179, 'doc:dir:manage', '管理目录', 'BUTTON', 178, NULL, NULL, 73, 0, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'doc:dir:manage');

INSERT INTO sys_permission (id, code, name, type, parent_id, path, icon, sort_order, deleted, version)
SELECT 180, 'doc:file:upload', '上传文件', 'BUTTON', 178, NULL, NULL, 74, 0, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'doc:file:upload');

INSERT INTO sys_permission (id, code, name, type, parent_id, path, icon, sort_order, deleted, version)
SELECT 181, 'doc:file:edit', '编辑/重命名/锁', 'BUTTON', 178, NULL, NULL, 75, 0, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'doc:file:edit');

INSERT INTO sys_permission (id, code, name, type, parent_id, path, icon, sort_order, deleted, version)
SELECT 182, 'doc:file:delete', '删除文件', 'BUTTON', 178, NULL, NULL, 76, 0, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'doc:file:delete');

INSERT INTO sys_permission (id, code, name, type, parent_id, path, icon, sort_order, deleted, version)
SELECT 183, 'doc:permission:manage', '授权管理', 'BUTTON', 178, NULL, NULL, 77, 0, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'doc:permission:manage');

-- ----------------------------------------------------------------------
-- 二、角色-权限矩阵（sys_role_permission，共 11 条）
--     角色：1=SYS_ADMIN、2=FINANCE、3=STAFF
-- ----------------------------------------------------------------------
INSERT INTO sys_role_permission (id, role_id, permission_id)
SELECT 1111, 1, 178
WHERE NOT EXISTS (SELECT 1 FROM sys_role_permission WHERE role_id = 1 AND permission_id = 178);

INSERT INTO sys_role_permission (id, role_id, permission_id)
SELECT 1112, 2, 178
WHERE NOT EXISTS (SELECT 1 FROM sys_role_permission WHERE role_id = 2 AND permission_id = 178);

INSERT INTO sys_role_permission (id, role_id, permission_id)
SELECT 1113, 3, 178
WHERE NOT EXISTS (SELECT 1 FROM sys_role_permission WHERE role_id = 3 AND permission_id = 178);

INSERT INTO sys_role_permission (id, role_id, permission_id)
SELECT 1114, 1, 179
WHERE NOT EXISTS (SELECT 1 FROM sys_role_permission WHERE role_id = 1 AND permission_id = 179);

INSERT INTO sys_role_permission (id, role_id, permission_id)
SELECT 1115, 1, 180
WHERE NOT EXISTS (SELECT 1 FROM sys_role_permission WHERE role_id = 1 AND permission_id = 180);

INSERT INTO sys_role_permission (id, role_id, permission_id)
SELECT 1116, 2, 180
WHERE NOT EXISTS (SELECT 1 FROM sys_role_permission WHERE role_id = 2 AND permission_id = 180);

INSERT INTO sys_role_permission (id, role_id, permission_id)
SELECT 1117, 3, 180
WHERE NOT EXISTS (SELECT 1 FROM sys_role_permission WHERE role_id = 3 AND permission_id = 180);

INSERT INTO sys_role_permission (id, role_id, permission_id)
SELECT 1118, 1, 181
WHERE NOT EXISTS (SELECT 1 FROM sys_role_permission WHERE role_id = 1 AND permission_id = 181);

INSERT INTO sys_role_permission (id, role_id, permission_id)
SELECT 1119, 3, 181
WHERE NOT EXISTS (SELECT 1 FROM sys_role_permission WHERE role_id = 3 AND permission_id = 181);

INSERT INTO sys_role_permission (id, role_id, permission_id)
SELECT 1120, 1, 182
WHERE NOT EXISTS (SELECT 1 FROM sys_role_permission WHERE role_id = 1 AND permission_id = 182);

INSERT INTO sys_role_permission (id, role_id, permission_id)
SELECT 1121, 1, 183
WHERE NOT EXISTS (SELECT 1 FROM sys_role_permission WHERE role_id = 1 AND permission_id = 183);
