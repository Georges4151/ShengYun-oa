package com.ceams.model.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GeneralLedger {

    private Long id;

    private String accountName;

    private String accountNumber;

    private String bankName;

    private BigDecimal balance;

    private Integer accountType;

    private String summary;

    private Date transactionTime;

    private Integer transactionType;

    private BigDecimal amount;

    private Integer status;

    private Date createTime;

    private Date updateTime;
}
