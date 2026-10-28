package com.ceams.module.bank.vo;

import com.ceams.module.bank.enums.BankStatementStatus;
import com.ceams.module.bank.enums.TransactionType;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 银行流水视图对象。
 */
@Data
public class BankStatementVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long accountId;

    private LocalDateTime transactionDate;

    private BigDecimal amount;

    private TransactionType type;

    private String description;

    private String counterparty;

    private String bankReference;

    private BigDecimal balance;

    private BankStatementStatus status;

    private LocalDateTime createTime;
}
