-- ======================================================================
-- V7：bank 域增量 —— 核对 V1 已建三表及索引
--
-- 说明：
--   V1 已创建 biz_bank_statement / biz_general_ledger /
--   biz_reconciliation_result 三表及必要索引（account/date/status/
--   account_name/time/statement/transaction），本阶段无需新增列或索引，
--   故仅保留一条无害语句以满足 Flyway 迁移文件需至少一条有效语句的要求。
-- ======================================================================

SELECT 1;
