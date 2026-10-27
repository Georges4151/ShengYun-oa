-- =====================================================
-- 修复 stamp_positions 列数据类型问题
-- 将 TEXT 改为 LONGTEXT 以支持存储多个盖章位置的 JSON 数据
-- 执行时间：2026-03-24
-- =====================================================

USE ceams;

-- 查看当前表结构
SELECT '修改前的表结构:' AS info;
DESC stamp_approvals;

-- 修改 stamp_positions 列从 TEXT 改为 LONGTEXT
ALTER TABLE stamp_approvals 
MODIFY COLUMN stamp_positions LONGTEXT COMMENT '印章位置信息JSON';

-- 验证修改是否成功
SELECT '修改后的表结构:' AS info;
DESC stamp_approvals;

-- 显示成功信息
SELECT 'stamp_positions 列已成功修改为 LONGTEXT 类型' AS result;
