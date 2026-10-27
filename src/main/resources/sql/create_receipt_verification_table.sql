-- 创建票据核销记录表
CREATE TABLE receipt_verification (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '核销记录ID',
    receipt_id INT NOT NULL COMMENT '关联的票据ID',
    payment_account_id INT NOT NULL COMMENT '支付账户ID',
    amount DECIMAL(10, 2) NOT NULL COMMENT '核销金额',
    payment_method VARCHAR(50) NOT NULL COMMENT '支付方式',
    verification_time DATETIME NOT NULL COMMENT '核销时间',
    verified_by INT NOT NULL COMMENT '核销人ID',
    remark VARCHAR(255) COMMENT '备注信息',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (receipt_id) REFERENCES receipt(id) ON DELETE CASCADE,
    FOREIGN KEY (payment_account_id) REFERENCES payment_account(id) ON DELETE CASCADE
);
