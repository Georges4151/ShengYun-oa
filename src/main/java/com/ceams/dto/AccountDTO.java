package com.ceams.dto;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 账目数据传输对象
 * 只包含允许客户端设置的字段，防止批量赋值攻击
 */
public class AccountDTO {
    private Integer projectId;
    private Integer categoryId;
    private Integer contactId;
    private Integer materialId;
    private Integer subcontractorId;
    private Integer receiptId;
    private Integer paymentAccountId;
    private Integer contractId;
    private Date transactionDate;
    private BigDecimal amount;
    private String type;
    private String paymentType;
    private String description;
    private String status;

    public Integer getProjectId() {
        return projectId;
    }

    public void setProjectId(Integer projectId) {
        this.projectId = projectId;
    }

    public Integer getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Integer categoryId) {
        this.categoryId = categoryId;
    }

    public Integer getContactId() {
        return contactId;
    }

    public void setContactId(Integer contactId) {
        this.contactId = contactId;
    }

    public Integer getMaterialId() {
        return materialId;
    }

    public void setMaterialId(Integer materialId) {
        this.materialId = materialId;
    }

    public Integer getSubcontractorId() {
        return subcontractorId;
    }

    public void setSubcontractorId(Integer subcontractorId) {
        this.subcontractorId = subcontractorId;
    }

    public Integer getReceiptId() {
        return receiptId;
    }

    public void setReceiptId(Integer receiptId) {
        this.receiptId = receiptId;
    }

    public Integer getPaymentAccountId() {
        return paymentAccountId;
    }

    public void setPaymentAccountId(Integer paymentAccountId) {
        this.paymentAccountId = paymentAccountId;
    }

    public Integer getContractId() {
        return contractId;
    }

    public void setContractId(Integer contractId) {
        this.contractId = contractId;
    }

    public Date getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(Date transactionDate) {
        this.transactionDate = transactionDate;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPaymentType() {
        return paymentType;
    }

    public void setPaymentType(String paymentType) {
        this.paymentType = paymentType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
