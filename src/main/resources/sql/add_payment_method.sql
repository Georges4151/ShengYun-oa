-- 添加支付方式字段到票据表
ALTER TABLE receipts ADD COLUMN payment_method VARCHAR(50) DEFAULT NULL COMMENT '支付方式: cash(现金), bank_transfer(银行转账), wechat(微信), alipay(支付宝), other(其他)';
