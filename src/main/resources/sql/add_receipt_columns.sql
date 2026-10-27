-- 添加票据表的报票人和归属项目字段
ALTER TABLE receipts
ADD COLUMN reporter_id INT NULL COMMENT '报票人ID',
ADD COLUMN project_id INT NULL COMMENT '归属项目ID';

-- 如果需要，可以添加外键约束
-- ALTER TABLE receipts
-- ADD CONSTRAINT fk_receipt_reporter FOREIGN KEY (reporter_id) REFERENCES users(id),
-- ADD CONSTRAINT fk_receipt_project FOREIGN KEY (project_id) REFERENCES projects(id);
