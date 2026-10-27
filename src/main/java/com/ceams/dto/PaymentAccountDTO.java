package com.ceams.dto;

import java.math.BigDecimal;

/**
 * PaymentAccount DTO - 只包含允许客户端设置的字段
 * 防止批量赋值攻击，敏感字段（id、createTime、updateTime）由服务端设置
 */
public class PaymentAccountDTO {
    private String name;
    private String type;
    private BigDecimal balance;
    private String description;

    // Getter and Setter methods
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
}
