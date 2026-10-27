-- 添加支付状态字段到票据表
ALTER TABLE receipts ADD COLUMN payment_status VARCHAR(50) NOT NULL DEFAULT 'unpaid' COMMENT '支付状态: unpaid(未支付), paid(已支付), partially_paid(部分支付)';
-- 添加支付金额字段
ALTER TABLE receipts ADD COLUMN paid_amount DECIMAL(12,2) DEFAULT 0 COMMENT '已支付金额';
-- 添加支付时间字段
ALTER TABLE receipts ADD COLUMN paid_at DATETIME COMMENT '支付时间';
-- 添加支付人字段
ALTER TABLE receipts ADD COLUMN paid_by INT COMMENT '支付人ID';
-- 添加外键约束
ALTER TABLE receipts ADD CONSTRAINT fk_receipts_paid_by FOREIGN KEY (paid_by) REFERENCES users(user_id);
