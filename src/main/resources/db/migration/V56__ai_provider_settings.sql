-- ======================================================================
-- V56：AI 提供商配置（文本对话 TEXT / 票据视觉识别 VISION 分别配置）
--
-- 说明：
--   1. 新表 sys_ai_provider：purpose 唯一（TEXT / VISION 各一行），
--      provider 取值 OLLAMA / OPENAI_COMPAT；preset 取值
--      DEEPSEEK / DASHSCOPE / SILICONFLOW / OPENAI / CUSTOM。
--   2. api_key_cipher 保存 SensitiveEncryptor（AES-256-GCM）加密后的 Base64 密文，
--      长度 1000 足以覆盖超长 Key；禁止明文入库。
--   3. 全局开关复用 sys_config（config_key = ai.enabled，值为 true/false）；
--      记录缺失时由应用回退到 ceams.ai.enabled，保证升级后行为不变。
--   4. ID 续接 V55（V55 已占用 sys_permission 197、sys_role_permission 1139）：
--      本脚本 sys_permission 使用 198，sys_role_permission 使用 1140。
--      占用区间：sys_permission 198；sys_role_permission 1140。
--   5. 权限点 system:ai:config（BUTTON）挂到「系统管理」菜单 137（code=iam）下，
--      仅授予 SYS_ADMIN（role_id = 1）。
--   6. 所有 INSERT 使用 WHERE NOT EXISTS，保证脚本可重复执行。
--   7. 本脚本为增量迁移，入库后不得修改；结构变更另起更高版本号脚本。
-- ======================================================================

-- ----------------------------------------------------------------------
-- 一、AI 提供商配置表
-- ----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_ai_provider (
    id            BIGINT        NOT NULL                COMMENT '主键（雪花ID）',
    purpose       VARCHAR(20)   NOT NULL                COMMENT '用途：TEXT=文本对话 VISION=票据视觉识别',
    provider      VARCHAR(30)   NOT NULL                COMMENT '提供商：OLLAMA / OPENAI_COMPAT',
    preset        VARCHAR(30)   NOT NULL DEFAULT 'CUSTOM' COMMENT '内置预设编码：DEEPSEEK/DASHSCOPE/SILICONFLOW/OPENAI/CUSTOM',
    base_url      VARCHAR(500)  NULL     DEFAULT NULL   COMMENT '服务地址（仅允许 http/https）',
    api_key_cipher VARCHAR(1000) NULL    DEFAULT NULL   COMMENT 'API Key 密文（AES-256-GCM，Base64）',
    model         VARCHAR(200)  NULL     DEFAULT NULL   COMMENT '模型名',
    timeout_seconds INT         NOT NULL DEFAULT 120    COMMENT '请求读取超时（秒）',
    create_time   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by     BIGINT        NULL     DEFAULT NULL   COMMENT '创建人ID',
    update_by     BIGINT        NULL     DEFAULT NULL   COMMENT '更新人ID',
    deleted       TINYINT       NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    version       INT           NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_ai_provider_purpose (purpose)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 提供商配置';

-- ----------------------------------------------------------------------
-- 二、权限点（sys_permission）
-- ----------------------------------------------------------------------
INSERT INTO sys_permission (id, code, name, type, parent_id, path, icon, sort_order, deleted, version)
SELECT 198, 'system:ai:config', 'AI 提供商配置', 'BUTTON', 137, NULL, NULL, 44, 0, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'system:ai:config');

-- ----------------------------------------------------------------------
-- 三、角色-权限矩阵（sys_role_permission）
--     角色：1 = SYS_ADMIN
-- ----------------------------------------------------------------------
INSERT INTO sys_role_permission (id, role_id, permission_id)
SELECT 1140, 1, 198
WHERE NOT EXISTS (SELECT 1 FROM sys_role_permission WHERE role_id = 1 AND permission_id = 198);
