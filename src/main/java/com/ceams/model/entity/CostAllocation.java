package com.ceams.model.entity;

import java.math.BigDecimal;
import java.util.Date;

public class CostAllocation {
    private Integer id;
    private Integer accountRecordId;
    private String targetType;
    private Integer targetId;
    private BigDecimal allocationAmount;
    private BigDecimal allocationRatio;
    private Date createTime;
    private Date updateTime;
    private String accountDescription;
    private String targetName;
    private BigDecimal originalAmount;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getAccountRecordId() {
        return accountRecordId;
    }

    public void setAccountRecordId(Integer accountRecordId) {
        this.accountRecordId = accountRecordId;
    }

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public Integer getTargetId() {
        return targetId;
    }

    public void setTargetId(Integer targetId) {
        this.targetId = targetId;
    }

    public BigDecimal getAllocationAmount() {
        return allocationAmount;
    }

    public void setAllocationAmount(BigDecimal allocationAmount) {
        this.allocationAmount = allocationAmount;
    }

    public BigDecimal getAllocationRatio() {
        return allocationRatio;
    }

    public void setAllocationRatio(BigDecimal allocationRatio) {
        this.allocationRatio = allocationRatio;
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

    public String getAccountDescription() {
        return accountDescription;
    }

    public void setAccountDescription(String accountDescription) {
        this.accountDescription = accountDescription;
    }

    public String getTargetName() {
        return targetName;
    }

    public void setTargetName(String targetName) {
        this.targetName = targetName;
    }

    public BigDecimal getOriginalAmount() {
        return originalAmount;
    }

    public void setOriginalAmount(BigDecimal originalAmount) {
        this.originalAmount = originalAmount;
    }
}
