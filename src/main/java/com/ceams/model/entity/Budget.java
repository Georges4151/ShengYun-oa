package com.ceams.model.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class Budget {
    private Integer id;

    private String budgetName;

    private BigDecimal amount;

    private Integer projectId;

    private Integer contractId;

    private Date startDate;

    private Date endDate;

    private String status;

    private Date createTime;

    private Date updateTime;

    // 关联字段，用于界面显示
    private String projectName;
    private String contractName;

    // 计算字段
    private BigDecimal usedAmount;
    private BigDecimal remainingAmount;
    private BigDecimal usageRate;
}
