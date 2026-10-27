-- 创建支付账户表
CREATE TABLE IF NOT EXISTS payment_account (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL COMMENT '账户名称',
    type VARCHAR(50) NOT NULL COMMENT '账户类型（现金、银行、微信、支付宝等）',
    balance DECIMAL(18,2) DEFAULT 0 COMMENT '账户余额',
    description TEXT COMMENT '账户描述',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付账户表';

-- 创建支付交易记录表
CREATE TABLE IF NOT EXISTS payment_transaction (
    id INT AUTO_INCREMENT PRIMARY KEY,
    account_id INT NOT NULL COMMENT '关联的账户ID',
    receipt_id INT DEFAULT NULL COMMENT '关联的票据ID',
    amount DECIMAL(18,2) NOT NULL COMMENT '交易金额',
    type VARCHAR(20) NOT NULL COMMENT '交易类型（收入、支出）',
    transaction_date DATETIME NOT NULL COMMENT '交易日期',
    description TEXT COMMENT '交易描述',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (account_id) REFERENCES payment_account(id) ON DELETE CASCADE,
    FOREIGN KEY (receipt_id) REFERENCES receipt(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付交易记录表';

-- 创建索引
CREATE INDEX idx_payment_transaction_account_id ON payment_transaction(account_id);
CREATE INDEX idx_payment_transaction_receipt_id ON payment_transaction(receipt_id);
CREATE INDEX idx_payment_transaction_transaction_date ON payment_transaction(transaction_date);
CREATE INDEX idx_payment_transaction_type ON payment_transaction(type);