-- =====================================================================
-- V10__device_app_secret.sql
-- 阶段 4（N5 后半）：AppSecret 设备绑定 —— 给 sys_user_device 增加设备签名密钥列。
--
-- 说明：
--   1. 签名校验由「全局共享密钥」切换为「按设备 AppSecret」校验，
--      每个已绑定设备持有独立 AppSecret（库内 AES-256-GCM 加密，复用敏感字段机制）。
--   2. 本迁移仅新增列，不改动既有数据；历史已绑定设备 app_secret 为 NULL，
--      客户端需「解绑后重新绑定」以生成新的 AppSecret。
--   3. 严禁改动 V1/V2/V4~V9。
-- =====================================================================

ALTER TABLE sys_user_device
    ADD COLUMN app_secret VARCHAR(255) NULL DEFAULT NULL COMMENT '设备签名密钥（AppSecret，库内加密）';
