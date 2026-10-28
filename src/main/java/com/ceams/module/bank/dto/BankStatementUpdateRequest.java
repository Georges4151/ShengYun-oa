package com.ceams.module.bank.dto;

import com.ceams.module.bank.enums.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 更新银行流水（对账状态由对账流程管理，不在此接口修改）。
 */
@Data
public class BankStatementUpdateRequest {

    private Long accountId;

    private LocalDateTime transactionDate;

    @DecimalMin(value = "0", message = "金额不能为负")
    private BigDecimal amount;

    private TransactionType type;

    private String description;

    private String counterparty;

    private String bankReference;

    private BigDecimal balance;
}
