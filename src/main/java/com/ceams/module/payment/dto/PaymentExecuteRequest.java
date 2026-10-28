package com.ceams.module.payment.dto;

import com.ceams.module.payment.enums.AccountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 付款执行请求（高危接口，需 HMAC 签名）。
 */
@Data
public class PaymentExecuteRequest {

    @NotNull(message = "付款账户ID不能为空")
    private Long accountId;

    private Long receiptId;

    private Long projectId;

    @NotNull(message = "金额不能为空")
    @DecimalMin(value = "0.01", message = "金额必须大于 0")
    private BigDecimal amount;

    @NotNull(message = "收支类型不能为空")
    private AccountType type;

    @NotNull(message = "交易时间不能为空")
    private LocalDateTime transactionDate;

    private String description;
}
