-- Create payment account table
CREATE TABLE IF NOT EXISTS payment_account (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    type VARCHAR(50) NOT NULL,
    balance DECIMAL(18,2) DEFAULT 0,
    description TEXT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Create payment transaction table
CREATE TABLE IF NOT EXISTS payment_transaction (
    id INT AUTO_INCREMENT PRIMARY KEY,
    account_id INT NOT NULL,
    receipt_id INT DEFAULT NULL,
    amount DECIMAL(18,2) NOT NULL,
    type VARCHAR(20) NOT NULL,
    transaction_date DATETIME NOT NULL,
    description TEXT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (account_id) REFERENCES payment_account(id) ON DELETE CASCADE,
    FOREIGN KEY (receipt_id) REFERENCES receipt(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Create indexes
CREATE INDEX idx_payment_transaction_account_id ON payment_transaction(account_id);
CREATE INDEX idx_payment_transaction_receipt_id ON payment_transaction(receipt_id);
CREATE INDEX idx_payment_transaction_transaction_date ON payment_transaction(transaction_date);
CREATE INDEX idx_payment_transaction_type ON payment_transaction(type);