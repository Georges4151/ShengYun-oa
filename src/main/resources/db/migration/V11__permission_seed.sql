-- ======================================================================
-- CEAMS 权限点种子数据（V11）
-- 说明：
--   1. 按现有业务模块建立权限树：顶层为 MENU，子节点为 BUTTON/API。
--   2. 主键采用固定小整数（100 起递增），避免与雪花 ID 冲突，也不占用 1~99。
--   3. sys_role_permission 主键从 1000 起，预置 SYS_ADMIN / FINANCE / STAFF 权限矩阵；
--      授予子权限时一并授予其所属 MENU，保证前端菜单可见。
--   4. 关联表物理删除且无逻辑删除列，故仅写 id/role_id/permission_id。
-- ======================================================================

-- ----------------------------------------------------------------------
-- 一、权限点（sys_permission）
-- ----------------------------------------------------------------------

-- 1.1 项目管理（project）
INSERT INTO sys_permission (id, code, name, type, parent_id, path, icon, sort_order, deleted, version) VALUES
    (100, 'project',                 '项目管理',   'MENU',   NULL, '/project',  'Folder', 1,  0, 0),
    (101, 'project:view',            '查看项目',   'API',    100,  NULL,        NULL,     2,  0, 0),
    (102, 'project:create',          '新建项目',   'BUTTON', 100,  NULL,        NULL,     3,  0, 0),
    (103, 'project:update',          '编辑项目',   'BUTTON', 100,  NULL,        NULL,     4,  0, 0),
    (104, 'project:delete',          '删除项目',   'BUTTON', 100,  NULL,        NULL,     5,  0, 0),
    (105, 'project:payment:approve', '进度款审批', 'BUTTON', 100,  NULL,        NULL,     6,  0, 0);

-- 1.2 报销票据（expense）
INSERT INTO sys_permission (id, code, name, type, parent_id, path, icon, sort_order, deleted, version) VALUES
    (106, 'expense',          '报销票据', 'MENU',   NULL, '/expense', 'Receipt', 7,  0, 0),
    (107, 'expense:view',     '查看报销', 'API',    106,  NULL,       NULL,      8,  0, 0),
    (108, 'expense:submit',   '上报报销', 'BUTTON', 106,  NULL,       NULL,      9,  0, 0),
    (109, 'expense:approve',  '审批报销', 'BUTTON', 106,  NULL,       NULL,      10, 0, 0),
    (110, 'expense:reject',   '驳回报销', 'BUTTON', 106,  NULL,       NULL,      11, 0, 0),
    (111, 'expense:verify',   '票据核验', 'BUTTON', 106,  NULL,       NULL,      12, 0, 0);

-- 1.3 资金收支（payment）
INSERT INTO sys_permission (id, code, name, type, parent_id, path, icon, sort_order, deleted, version) VALUES
    (112, 'payment',        '资金收支', 'MENU',   NULL, '/payment', 'Wallet', 13, 0, 0),
    (113, 'payment:view',   '查看收支', 'API',    112,  NULL,       NULL,     14, 0, 0),
    (114, 'payment:create', '登记收支', 'BUTTON', 112,  NULL,       NULL,     15, 0, 0),
    (115, 'payment:update', '编辑收支', 'BUTTON', 112,  NULL,       NULL,     16, 0, 0),
    (116, 'payment:delete', '删除收支', 'BUTTON', 112,  NULL,       NULL,     17, 0, 0),
    (117, 'payment:audit',  '审核收支', 'BUTTON', 112,  NULL,       NULL,     18, 0, 0);

-- 1.4 银行对账（bank）
INSERT INTO sys_permission (id, code, name, type, parent_id, path, icon, sort_order, deleted, version) VALUES
    (118, 'bank',            '银行对账', 'MENU',   NULL, '/bank', 'Bank', 19, 0, 0),
    (119, 'bank:view',       '查看对账', 'API',    118,  NULL,    NULL,   20, 0, 0),
    (120, 'bank:import',     '导入流水', 'BUTTON', 118,  NULL,    NULL,   21, 0, 0),
    (121, 'bank:match',      '手工匹配', 'BUTTON', 118,  NULL,    NULL,   22, 0, 0),
    (122, 'bank:reconcile',  '执行对账', 'BUTTON', 118,  NULL,    NULL,   23, 0, 0);

-- 1.5 电子印章（seal）
INSERT INTO sys_permission (id, code, name, type, parent_id, path, icon, sort_order, deleted, version) VALUES
    (123, 'seal',         '电子印章', 'MENU',   NULL, '/seal', 'Stamp', 24, 0, 0),
    (124, 'seal:view',    '查看印章', 'API',    123,  NULL,    NULL,    25, 0, 0),
    (125, 'seal:stamp',   '盖章',     'BUTTON', 123,  NULL,    NULL,    26, 0, 0),
    (126, 'seal:approve', '印章审批', 'BUTTON', 123,  NULL,    NULL,    27, 0, 0),
    (127, 'seal:grant',   '授权使用', 'BUTTON', 123,  NULL,    NULL,    28, 0, 0);

-- 1.6 基础数据（masterdata）
INSERT INTO sys_permission (id, code, name, type, parent_id, path, icon, sort_order, deleted, version) VALUES
    (128, 'masterdata',        '基础数据',     'MENU',   NULL, '/masterdata', 'Database', 29, 0, 0),
    (129, 'masterdata:view',   '查看基础数据', 'API',    128,  NULL,          NULL,       30, 0, 0),
    (130, 'masterdata:create', '新增基础数据', 'BUTTON', 128,  NULL,          NULL,       31, 0, 0),
    (131, 'masterdata:update', '编辑基础数据', 'BUTTON', 128,  NULL,          NULL,       32, 0, 0),
    (132, 'masterdata:delete', '删除基础数据', 'BUTTON', 128,  NULL,          NULL,       33, 0, 0);

-- 1.7 通知（notification）
INSERT INTO sys_permission (id, code, name, type, parent_id, path, icon, sort_order, deleted, version) VALUES
    (133, 'notification',         '通知',     'MENU',   NULL, '/notification', 'Bell', 34, 0, 0),
    (134, 'notification:view',    '查看通知', 'API',    133,  NULL,            NULL,   35, 0, 0),
    (135, 'notification:send',    '发送通知', 'BUTTON', 133,  NULL,            NULL,   36, 0, 0),
    (136, 'notification:manage',  '通知配置', 'BUTTON', 133,  NULL,            NULL,   37, 0, 0);

-- 1.8 系统管理（iam）
INSERT INTO sys_permission (id, code, name, type, parent_id, path, icon, sort_order, deleted, version) VALUES
    (137, 'iam',                   '系统管理', 'MENU',   NULL, '/system', 'Setting', 38, 0, 0),
    (138, 'iam:user:manage',       '用户管理', 'BUTTON', 137,  NULL,      NULL,      39, 0, 0),
    (139, 'iam:role:manage',       '角色管理', 'BUTTON', 137,  NULL,      NULL,      40, 0, 0),
    (140, 'iam:permission:manage', '权限管理', 'BUTTON', 137,  NULL,      NULL,      41, 0, 0),
    (141, 'iam:device:manage',     '设备管理', 'BUTTON', 137,  NULL,      NULL,      42, 0, 0),
    (142, 'iam:log:view',          '日志查看', 'API',    137,  NULL,      NULL,      43, 0, 0);

-- ----------------------------------------------------------------------
-- 二、角色-权限矩阵（sys_role_permission）
-- ----------------------------------------------------------------------

-- 2.1 SYS_ADMIN（role 1）：全部权限点（100~142）
INSERT INTO sys_role_permission (id, role_id, permission_id) VALUES
    (1000, 1, 100), (1001, 1, 101), (1002, 1, 102), (1003, 1, 103), (1004, 1, 104),
    (1005, 1, 105), (1006, 1, 106), (1007, 1, 107), (1008, 1, 108), (1009, 1, 109),
    (1010, 1, 110), (1011, 1, 111), (1012, 1, 112), (1013, 1, 113), (1014, 1, 114),
    (1015, 1, 115), (1016, 1, 116), (1017, 1, 117), (1018, 1, 118), (1019, 1, 119),
    (1020, 1, 120), (1021, 1, 121), (1022, 1, 122), (1023, 1, 123), (1024, 1, 124),
    (1025, 1, 125), (1026, 1, 126), (1027, 1, 127), (1028, 1, 128), (1029, 1, 129),
    (1030, 1, 130), (1031, 1, 131), (1032, 1, 132), (1033, 1, 133), (1034, 1, 134),
    (1035, 1, 135), (1036, 1, 136), (1037, 1, 137), (1038, 1, 138), (1039, 1, 139),
    (1040, 1, 140), (1041, 1, 141), (1042, 1, 142);

-- 2.2 FINANCE（role 2）：项目查看/进度款、报销审批核验、资金收支、银行对账、通知
INSERT INTO sys_role_permission (id, role_id, permission_id) VALUES
    (1043, 2, 100), (1044, 2, 101), (1045, 2, 105),
    (1046, 2, 106), (1047, 2, 107), (1048, 2, 109), (1049, 2, 110), (1050, 2, 111),
    (1051, 2, 112), (1052, 2, 113), (1053, 2, 114), (1054, 2, 115), (1055, 2, 116), (1056, 2, 117),
    (1057, 2, 118), (1058, 2, 119), (1059, 2, 120), (1060, 2, 121), (1061, 2, 122),
    (1062, 2, 133), (1063, 2, 134), (1064, 2, 135);

-- 2.3 STAFF（role 3）：项目查看、报销查看/上报、通知
INSERT INTO sys_role_permission (id, role_id, permission_id) VALUES
    (1065, 3, 100), (1066, 3, 101),
    (1067, 3, 106), (1068, 3, 107), (1069, 3, 108),
    (1070, 3, 133), (1071, 3, 134);
