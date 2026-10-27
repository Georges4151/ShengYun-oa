-- =====================================================
-- 票据唯一性约束迁移脚本
-- 版本: V1.0.12
-- 日期: 2026-03-29
-- 描述: 添加票据唯一性约束，防止重复票据录入
-- =====================================================

-- 1. 添加文件哈希唯一索引
-- 用于防止完全相同的票据文件重复上传
ALTER TABLE receipts 
ADD UNIQUE INDEX uk_file_hash (file_hash);

-- 2. 添加业务字段组合唯一索引
-- 用于防止同一项目、同一日期、同一金额、同一类型的票据重复录入
-- 注意：如果已有重复数据，需要先清理重复数据后再执行此语句
-- 可以使用以下查询检查重复数据：
-- SELECT project_id, receipt_date, amount, type, COUNT(*) as count
-- FROM receipts
-- GROUP BY project_id, receipt_date, amount, type
-- HAVING COUNT(*) > 1;

ALTER TABLE receipts 
ADD UNIQUE INDEX uk_receipt_unique (project_id, receipt_date, amount, type);

-- 3. 如果上述语句因重复数据失败，可以使用以下步骤：
-- 步骤1: 查找并标记重复数据
-- CREATE TEMPORARY TABLE duplicate_receipts AS
-- SELECT MIN(id) as keep_id, project_id, receipt_date, amount, type
-- FROM receipts
-- GROUP BY project_id, receipt_date, amount, type
-- HAVING COUNT(*) > 1;

-- 步骤2: 删除重复数据（保留最早的一条）
-- DELETE r1 FROM receipts r1
-- INNER JOIN receipts r2 
-- ON r1.project_id = r2.project_id 
--    AND r1.receipt_date = r2.receipt_date 
--    AND r1.amount = r2.amount 
--    AND r1.type = r2.type
--    AND r1.id > r2.id;

-- 步骤3: 重新执行添加唯一索引的语句
-- ALTER TABLE receipts 
-- ADD UNIQUE INDEX uk_receipt_unique (project_id, receipt_date, amount, type);

-- =====================================================
-- 回滚脚本（如需回滚）
-- =====================================================
-- ALTER TABLE receipts DROP INDEX uk_file_hash;
-- ALTER TABLE receipts DROP INDEX uk_receipt_unique;
