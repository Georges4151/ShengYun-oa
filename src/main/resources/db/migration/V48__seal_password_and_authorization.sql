-- ======================================================================
-- V48：印章独立密码校验 + 印章级授权使用
--
-- 说明：
--   1. biz_electronic_seal 增加 password_hash 列，存 BCrypt 哈希；
--      password_protected 仍表示是否启用密码保护，二者配合使用。
--   2. 新建 biz_seal_authorization，管理员可将某枚印章授权给指定用户，
--      在 [start_time, end_time] 有效期内可提交用印申请与盖章。
--   3. 权限点 ID 续接 V46：sys_permission 从 192 起、sys_role_permission 从 1130 起；
--      三个权限点挂在电子印章菜单 123 下。
--   4. 角色授权：SYS_ADMIN（role_id=1）与 FINANCE（role_id=2）。
--   5. 权限种子使用 INSERT ... SELECT ... WHERE NOT EXISTS，可重复执行。
-- ======================================================================

-- ----------------------------------------------------------------------
-- 一、印章密码哈希列
-- ----------------------------------------------------------------------
ALTER TABLE biz_electronic_seal
    ADD COLUMN password_hash VARCHAR(100) NULL COMMENT '印章密码 BCrypt 哈希' AFTER password_protected;

-- ----------------------------------------------------------------------
-- 二、印章授权表
-- ----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS biz_seal_authorization (
    id          BIGINT       NOT NULL                COMMENT '主键（雪花ID）',
    seal_id     BIGINT       NOT NULL                COMMENT '印章ID（逻辑引用 biz_electronic_seal.id）',
    user_id     BIGINT       NOT NULL                COMMENT '被授权用户ID（逻辑引用 sys_user.id）',
    start_time  DATETIME     NOT NULL                COMMENT '授权开始时间',
    end_time    DATETIME     NOT NULL                COMMENT '授权结束时间',
    enabled     TINYINT      NOT NULL DEFAULT 1      COMMENT '是否启用：0否 1是',
    remark      VARCHAR(255) NULL     DEFAULT NULL   COMMENT '备注',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by   BIGINT       NULL     DEFAULT NULL   COMMENT '创建人ID',
    update_by   BIGINT       NULL     DEFAULT NULL   COMMENT '更新人ID',
    deleted     TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    version     INT          NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
    PRIMARY KEY (id),
    KEY idx_seal_auth_seal_user (seal_id, user_id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='印章授权使用';

-- ----------------------------------------------------------------------
-- 三、权限点（sys_permission）
-- ----------------------------------------------------------------------
INSERT INTO sys_permission (id, code, name, type, parent_id, path, icon, sort_order, deleted, version)
SELECT 192, 'seal:authorization:list', '查看印章授权', 'API', 123, NULL, NULL, 29, 0, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'seal:authorization:list');

INSERT INTO sys_permission (id, code, name, type, parent_id, path, icon, sort_order, deleted, version)
SELECT 193, 'seal:authorization:create', '新增印章授权', 'BUTTON', 123, NULL, NULL, 30, 0, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'seal:authorization:create');

INSERT INTO sys_permission (id, code, name, type, parent_id, path, icon, sort_order, deleted, version)
SELECT 194, 'seal:authorization:revoke', '撤销印章授权', 'BUTTON', 123, NULL, NULL, 31, 0, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'seal:authorization:revoke');

-- ----------------------------------------------------------------------
-- 四、角色-权限矩阵（sys_role_permission）
--     角色：1=SYS_ADMIN、2=FINANCE
-- ----------------------------------------------------------------------
INSERT INTO sys_role_permission (id, role_id, permission_id)
SELECT 1130, 1, 192
WHERE NOT EXISTS (SELECT 1 FROM sys_role_permission WHERE role_id = 1 AND permission_id = 192);

INSERT INTO sys_role_permission (id, role_id, permission_id)
SELECT 1131, 1, 193
WHERE NOT EXISTS (SELECT 1 FROM sys_role_permission WHERE role_id = 1 AND permission_id = 193);

INSERT INTO sys_role_permission (id, role_id, permission_id)
SELECT 1132, 1, 194
WHERE NOT EXISTS (SELECT 1 FROM sys_role_permission WHERE role_id = 1 AND permission_id = 194);

INSERT INTO sys_role_permission (id, role_id, permission_id)
SELECT 1133, 2, 192
WHERE NOT EXISTS (SELECT 1 FROM sys_role_permission WHERE role_id = 2 AND permission_id = 192);

INSERT INTO sys_role_permission (id, role_id, permission_id)
SELECT 1134, 2, 193
WHERE NOT EXISTS (SELECT 1 FROM sys_role_permission WHERE role_id = 2 AND permission_id = 193);

INSERT INTO sys_role_permission (id, role_id, permission_id)
SELECT 1135, 2, 194
WHERE NOT EXISTS (SELECT 1 FROM sys_role_permission WHERE role_id = 2 AND permission_id = 194);
