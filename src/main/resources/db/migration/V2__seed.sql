-- ======================================================================
-- CEAMS 种子数据（V2）
-- 说明：
--   1. 初始化默认管理员、初始角色、默认印章、系统配置项。
--   2. 所有主键采用固定小整数，与雪花ID（应用生成）无冲突。
--   3. 管理员初始密码 admin123（BCrypt）。生产环境请在首次登录后强制修改。
--   4. 枚举均使用代码值（不落中文），中文仅用于 name 等展示字段。
-- ======================================================================

-- ----------------------------------------------------------------------
-- 一、初始角色（sys_role）
-- ----------------------------------------------------------------------
INSERT INTO sys_role (id, code, name, description, status) VALUES
    (1, 'SYS_ADMIN', '系统管理员', '拥有系统全部权限', 'ACTIVE'),
    (2, 'FINANCE',   '财务',       '票据核验、资金收支、对账', 'ACTIVE'),
    (3, 'STAFF',     '普通员工',   '票据上报、浏览项目', 'ACTIVE');

-- ----------------------------------------------------------------------
-- 二、默认管理员（sys_user + sys_user_role）
-- ----------------------------------------------------------------------
INSERT INTO sys_user (id, username, password_hash, name, status) VALUES
    (1, 'admin',
     '$2b$10$d9JLCOaKdCGYsTp9lnf1vuDhxdDK25uDLAyN8CtfyYnORLMfCkahC',
     '系统管理员', 'ACTIVE');

-- 管理员绑定系统管理员角色
INSERT INTO sys_user_role (id, user_id, role_id) VALUES
    (1, 1, 1);

-- ----------------------------------------------------------------------
-- 三、默认印章（biz_electronic_seal，尚未绑定真实印章文件）
-- ----------------------------------------------------------------------
INSERT INTO biz_electronic_seal (id, seal_name, seal_type, seal_code, status, storage_type) VALUES
    (1, '公司公章',     'COMPANY_SEAL',  'SEAL_COMPANY',  'ACTIVE', 'FILE'),
    (2, '合同专用章',   'CONTRACT_SEAL', 'SEAL_CONTRACT', 'ACTIVE', 'FILE'),
    (3, '财务专用章',   'FINANCE_SEAL',  'SEAL_FINANCE',  'ACTIVE', 'FILE'),
    (4, '法定代表人章', 'LEGAL_SEAL',    'SEAL_LEGAL',    'ACTIVE', 'FILE');

-- ----------------------------------------------------------------------
-- 四、系统配置项（sys_config）
-- ----------------------------------------------------------------------
INSERT INTO sys_config (id, config_key, config_value, config_type, description) VALUES
    (1, 'system.name',                    'CEAMS',                              'STRING', '系统名称'),
    (2, 'seal.default.types',             '["COMPANY_SEAL","CONTRACT_SEAL","FINANCE_SEAL","LEGAL_SEAL"]', 'JSON', '默认印章类型'),
    (3, 'security.access_token.ttl',      '900',                                'INT',    '访问令牌有效期（秒）'),
    (4, 'security.refresh_token.ttl',     '604800',                             'INT',    '刷新令牌有效期（秒）'),
    (5, 'security.sign.window.minutes',   '5',                                  'INT',    '高危接口签名时间窗口（分钟）'),
    (6, 'paging.default.size',            '20',                                 'INT',    '默认分页大小'),
    (7, 'paging.max.size',                '100',                                'INT',    '分页上限');