package com.ceams.module.payment.dto;

import com.ceams.module.payment.enums.AccountType;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/** 更新收支流水（仅草稿态可编辑）。 */
@Data
public class AccountUpdateRequest {

    private Long projectId;

    private LocalDateTime transactionDate;

    @DecimalMin(value = "0", message = "金额不能为负")
    private BigDecimal amount;

    private AccountType type;

    private Long receiptId;

    private String description;

    private Long paymentAccountId;

    private Long contractId;

    private String paymentType;

    private Long categoryId;

    private Long contactId;

    private Long materialId;

    private Long subcontractorId;
}
