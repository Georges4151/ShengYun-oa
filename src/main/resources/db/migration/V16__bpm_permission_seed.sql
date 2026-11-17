-- ======================================================================
-- V16：BPM 权限点种子数据
--
-- 说明：
--   1. 沿用 V11__permission_seed.sql 的显式 ID 风格：V11 权限点占 100~142，
--      本迁移从 143 起递增；sys_role_permission 从 1072 起递增，均避开已有 ID。
--   2. 六个权限码挂在同一个 MENU 父节点（bpm）下；授予子权限时一并授予
--      父 MENU，保证前端菜单可见。
--   3. SYS_ADMIN（role_id=1）全部授权；FINANCE（role_id=2）授予实例查看与流程报表，
--      其余角色沿用现有矩阵，不在此扩大授权。
-- ======================================================================

-- ----------------------------------------------------------------------
-- 一、权限点（sys_permission）
-- ----------------------------------------------------------------------
INSERT INTO sys_permission (id, code, name, type, parent_id, path, icon, sort_order, deleted, version) VALUES
    (143, 'bpm',                 '流程审批',   'MENU',   NULL, '/bpm', 'Workflow', 44, 0, 0),
    (144, 'bpm:template:view',   '查看流程模板', 'API',    143,  NULL,   NULL,       45, 0, 0),
    (145, 'bpm:template:manage', '管理流程模板', 'BUTTON', 143,  NULL,   NULL,       46, 0, 0),
    (146, 'bpm:instance:view',   '查看流程实例', 'API',    143,  NULL,   NULL,       47, 0, 0),
    (147, 'bpm:task:operate',    '处理审批任务', 'BUTTON', 143,  NULL,   NULL,       48, 0, 0),
    (148, 'bpm:task:delegate',   '改派审批任务', 'BUTTON', 143,  NULL,   NULL,       49, 0, 0),
    (149, 'bpm:report:view',     '查看流程报表', 'API',    143,  NULL,   NULL,       50, 0, 0);

-- ----------------------------------------------------------------------
-- 二、角色-权限矩阵（sys_role_permission）
-- ----------------------------------------------------------------------

-- 2.1 SYS_ADMIN（role 1）：BPM 菜单与六个权限点
INSERT INTO sys_role_permission (id, role_id, permission_id) VALUES
    (1072, 1, 143),
    (1073, 1, 144),
    (1074, 1, 145),
    (1075, 1, 146),
    (1076, 1, 147),
    (1077, 1, 148),
    (1078, 1, 149);

-- 2.2 FINANCE（role_id=2）：BPM 菜单 + 实例查看 + 流程报表
INSERT INTO sys_role_permission (id, role_id, permission_id) VALUES
    (1079, 2, 143),
    (1080, 2, 146),
    (1081, 2, 149);
