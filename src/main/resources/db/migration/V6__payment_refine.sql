-- ======================================================================
-- V6：payment 域增量 —— 补齐 M3 所需的缺失索引
--
-- 说明：
--   1) biz_payment_transaction 已含 account/receipt/date 索引，但缺少
--      project_id 索引（付款执行、项目维度查询需要）。
--   2) biz_account 已含 project/receipt/payment_account/status 索引，
--      但缺少 allocation_status 索引（成本分摊状态查询需要）。
--   均为 V1 缺失的增量索引，不改已存在列、不删表。
-- ======================================================================

ALTER TABLE biz_payment_transaction
    ADD KEY idx_biz_payment_transaction_project (project_id);

ALTER TABLE biz_account
    ADD KEY idx_biz_account_allocation_status (allocation_status);
