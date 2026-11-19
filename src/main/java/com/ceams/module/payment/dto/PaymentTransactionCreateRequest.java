package com.ceams.module.payment.dto;

import com.ceams.module.payment.enums.AccountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/** 新建付款交易。 */
@Data
public class PaymentTransactionCreateRequest {

    @NotNull(message = "付款账户ID不能为空")
    private Long accountId;

    private Long receiptId;

    private Long projectId;

    @NotNull(message = "金额不能为空")
    @DecimalMin(value = "0", message = "金额不能为负")
    private BigDecimal amount;

    @NotNull(message = "收支类型不能为空")
    private AccountType type;

    @NotNull(message = "交易时间不能为空")
    private LocalDateTime transactionDate;

    private String description;
}
