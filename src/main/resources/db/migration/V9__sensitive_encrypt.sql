-- =====================================================================
-- V9__sensitive_encrypt.sql
-- 阶段 4（N5）：敏感字段加密与脱敏 —— 加宽敏感列以容纳 Base64 密文。
--
-- 说明：
--   1. 本迁移仅加宽列宽并更新 COMMENT，不改动既有数据（当前无明文敏感数据）。
--   2. 仅修改 V1 中定义的 9 个敏感列；严禁改动 V1/V2/V4~V8。
--   3. 密文格式：v1: + Base64( IV(12B) || ciphertext || GCM Tag(16B) )。
--      原 phone 最长 20、税号最长 50、账号最长 100，加密后分别约 60/110/180 字符，
--      故加宽到 200/128/200 足矣。
-- =====================================================================

-- 1. 系统用户手机号
ALTER TABLE sys_user
    MODIFY COLUMN phone VARCHAR(200) NULL DEFAULT NULL COMMENT '手机号【已加密】';

-- 2. 往来单位：手机号 / 税号 / 银行账号
ALTER TABLE biz_contact
    MODIFY COLUMN phone VARCHAR(200) NULL DEFAULT NULL COMMENT '手机号【已加密】',
    MODIFY COLUMN tax_identification_number VARCHAR(128) NULL DEFAULT NULL COMMENT '税号【已加密】',
    MODIFY COLUMN bank_account VARCHAR(200) NULL DEFAULT NULL COMMENT '银行账号【已加密】';

-- 3. 分包商：手机号 / 税号 / 银行账号
ALTER TABLE biz_subcontractor
    MODIFY COLUMN phone VARCHAR(200) NULL DEFAULT NULL COMMENT '手机号【已加密】',
    MODIFY COLUMN tax_identification_number VARCHAR(128) NULL DEFAULT NULL COMMENT '税号【已加密】',
    MODIFY COLUMN bank_account VARCHAR(200) NULL DEFAULT NULL COMMENT '银行账号【已加密】';

-- 4. 付款账户账号
ALTER TABLE biz_payment_account
    MODIFY COLUMN account_number VARCHAR(200) NULL DEFAULT NULL COMMENT '账号【已加密】';

-- 5. 总账账号
ALTER TABLE biz_general_ledger
    MODIFY COLUMN account_number VARCHAR(200) NULL DEFAULT NULL COMMENT '账号【已加密】';
