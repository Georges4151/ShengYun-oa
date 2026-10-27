-- =====================================================
-- 票据编号字段迁移脚本
-- 版本: V1.0.13
-- 日期: 2026-03-30
-- 描述: 添加票据编号字段，修改唯一性约束为仅以票据编号为唯一标识
-- =====================================================

-- 1. 添加票据编号字段
ALTER TABLE receipts 
ADD COLUMN receipt_number VARCHAR(100) COMMENT '票据编号';

-- 2. 删除旧的组合唯一索引（如果存在）
-- 注意：先检查是否存在，如果不存在会报错，可以忽略
-- ALTER TABLE receipts DROP INDEX uk_receipt_unique;

-- 3. 添加票据编号唯一索引
ALTER TABLE receipts 
ADD UNIQUE INDEX uk_receipt_number (receipt_number);

-- 4. 为现有数据生成票据编号（如果需要）
-- 可以使用以下SQL为现有记录生成票据编号
-- UPDATE receipts SET receipt_number = CONCAT('RCP-', LPAD(id, 8, '0')) WHERE receipt_number IS NULL;

-- =====================================================
-- 回滚脚本（如需回滚）
-- =====================================================
-- ALTER TABLE receipts DROP INDEX uk_receipt_number;
-- ALTER TABLE receipts DROP COLUMN receipt_number;
