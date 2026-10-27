-- 为票据表添加描述字段
-- 执行日期: 2026-03-16

USE ceams;

-- 添加 description 列到 receipts 表
ALTER TABLE receipts 
ADD COLUMN description TEXT COMMENT '票据描述' AFTER type;

-- 验证列是否添加成功
SHOW COLUMNS FROM receipts LIKE 'description';
