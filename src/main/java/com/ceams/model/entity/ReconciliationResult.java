package com.ceams.model.entity;

import java.math.BigDecimal;
import java.util.Date;

public class ReconciliationResult {
    private Integer id;
    private Integer accountId;
    private Integer statementId;
    private Integer transactionId;
    private String status; // matched, unmatched, partial_match
    private String discrepancyType; // amount_mismatch, date_mismatch, description_mismatch, missing_in_bank, missing_in_system
    private BigDecimal systemAmount;
    private BigDecimal bankAmount;
    private Date systemDate;
    private Date bankDate;
    private String systemDescription;
    private String bankDescription;
    private String resolutionStatus; // unresolved, resolved, ignored
    private String resolutionNotes;
    private Date reconciliationDate;
    private Date createTime;
    private Date updateTime;
    
    // 关联字段，用于界面显示
    private String accountName;
    private String statementReference;
    private String transactionReference;
    
    public Integer getId() {
        return id;
    }
    
    public void setId(Integer id) {
        this.id = id;
    }
    
    public Integer getAccountId() {
        return accountId;
    }
    
    public void setAccountId(Integer accountId) {
        this.accountId = accountId;
    }
    
    public Integer getStatementId() {
        return statementId;
    }
    
    public void setStatementId(Integer statementId) {
        this.statementId = statementId;
    }
    
    public Integer getTransactionId() {
        return transactionId;
    }
    
    public void setTransactionId(Integer transactionId) {
        this.transactionId = transactionId;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getDiscrepancyType() {
        return discrepancyType;
    }
    
    public void setDiscrepancyType(String discrepancyType) {
        this.discrepancyType = discrepancyType;
    }
    
    public BigDecimal getSystemAmount() {
        return systemAmount;
    }
    
    public void setSystemAmount(BigDecimal systemAmount) {
        this.systemAmount = systemAmount;
    }
    
    public BigDecimal getBankAmount() {
        return bankAmount;
    }
    
    public void setBankAmount(BigDecimal bankAmount) {
        this.bankAmount = bankAmount;
    }
    
    public Date getSystemDate() {
        return systemDate;
    }
    
    public void setSystemDate(Date systemDate) {
        this.systemDate = systemDate;
    }
    
    public Date getBankDate() {
        return bankDate;
    }
    
    public void setBankDate(Date bankDate) {
        this.bankDate = bankDate;
    }
    
    public String getSystemDescription() {
        return systemDescription;
    }
    
    public void setSystemDescription(String systemDescription) {
        this.systemDescription = systemDescription;
    }
    
    public String getBankDescription() {
        return bankDescription;
    }
    
    public void setBankDescription(String bankDescription) {
        this.bankDescription = bankDescription;
    }
    
    public String getResolutionStatus() {
        return resolutionStatus;
    }
    
    public void setResolutionStatus(String resolutionStatus) {
        this.resolutionStatus = resolutionStatus;
    }
    
    public String getResolutionNotes() {
        return resolutionNotes;
    }
    
    public void setResolutionNotes(String resolutionNotes) {
        this.resolutionNotes = resolutionNotes;
    }
    
    public Date getReconciliationDate() {
        return reconciliationDate;
    }
    
    public void setReconciliationDate(Date reconciliationDate) {
        this.reconciliationDate = reconciliationDate;
    }
    
    public Date getCreateTime() {
        return createTime;
    }
    
    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
    
    public Date getUpdateTime() {
        return updateTime;
    }
    
    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }
    
    public String getAccountName() {
        return accountName;
    }
    
    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }
    
    public String getStatementReference() {
        return statementReference;
    }
    
    public void setStatementReference(String statementReference) {
        this.statementReference = statementReference;
    }
    
    public String getTransactionReference() {
        return transactionReference;
    }
    
    public void setTransactionReference(String transactionReference) {
        this.transactionReference = transactionReference;
    }
}
