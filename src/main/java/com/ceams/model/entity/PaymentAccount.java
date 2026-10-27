package com.ceams.model.entity;

import java.math.BigDecimal;
import java.util.Date;

public class PaymentAccount {
    private Integer id;
    private String name;
    private String type;
    private BigDecimal balance;
    private String description;
    private Date createTime;
    private Date updateTime;

    // Getter and Setter methods
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    // 获取中文账户类型
    public String getTypeText() {
        switch (type) {
            case "cash":
                return "现金";
            case "bank":
                return "银行";
            case "wechat":
                return "微信";
            case "alipay":
                return "支付宝";
            case "other":
                return "其他";
            default:
                return type;
        }
    }

    @Override
    public String toString() {
        return name;
    }
}