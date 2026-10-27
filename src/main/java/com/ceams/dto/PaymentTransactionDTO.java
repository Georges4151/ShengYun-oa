package com.ceams.dto;

import java.math.BigDecimal;
import java.util.Date;

/**
 * PaymentTransaction DTO - 只包含允许客户端设置的字段
 * 防止批量赋值攻击，敏感字段（id、createTime、receiptId、projectId）由服务端设置
 */
public class PaymentTransactionDTO {
    private Integer accountId;
    private Integer receiptId;
    private String type;
    private BigDecimal amount;
    private String description;
    private Date transactionDate;

    public Integer getReceiptId() {
        return receiptId;
    }

    public void setReceiptId(Integer receiptId) {
        this.receiptId = receiptId;
    }

    // Getter and Setter methods
    public Integer getAccountId() {
        return accountId;
    }

    public void setAccountId(Integer accountId) {
        this.accountId = accountId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Date getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(Date transactionDate) {
        this.transactionDate = transactionDate;
    }
}
