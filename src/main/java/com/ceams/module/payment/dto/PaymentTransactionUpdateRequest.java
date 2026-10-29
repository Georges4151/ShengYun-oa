package com.ceams.module.payment.dto;

import com.ceams.module.payment.enums.AccountType;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/** 更新付款交易。 */
@Data
public class PaymentTransactionUpdateRequest {

    private Long accountId;

    private Long receiptId;

    private Long projectId;

    @DecimalMin(value = "0", message = "金额不能为负")
    private BigDecimal amount;

    private AccountType type;

    private LocalDateTime transactionDate;

    private String description;
}
