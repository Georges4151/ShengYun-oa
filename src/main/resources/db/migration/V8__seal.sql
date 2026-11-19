-- ======================================================================
-- V8：seal 域增量 —— 核对 V1 已建五表及索引
--
-- 说明：
--   V1 已创建 biz_electronic_seal / biz_electronic_seal_application /
--   biz_stamp_approval / biz_electronic_seal_stamp / biz_electronic_seal_usage
--   五表及必要索引（type/status/seal/applicant/stamp/project/user），
--   本阶段无需新增列或索引，故仅保留一条无害语句以满足 Flyway
--   迁移文件需至少一条有效语句的要求。
-- ======================================================================

SELECT 1;
