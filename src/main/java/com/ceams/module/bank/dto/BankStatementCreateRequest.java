package com.ceams.module.bank.dto;

import com.ceams.module.bank.enums.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/** 新建银行流水。 */
@Data
public class BankStatementCreateRequest {

    @NotNull(message = "付款账户ID不能为空")
    private Long accountId;

    @NotNull(message = "交易日期不能为空")
    private LocalDateTime transactionDate;

    @NotNull(message = "金额不能为空")
    @DecimalMin(value = "0", message = "金额不能为负")
    private BigDecimal amount;

    @NotNull(message = "收支类型不能为空")
    private TransactionType type;

    private String description;

    private String counterparty;

    private String bankReference;

    private BigDecimal balance;
}
