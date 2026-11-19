package com.ceams.module.payment.dto;

import com.ceams.module.payment.enums.AccountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/** 新建收支流水。 */
@Data
public class AccountCreateRequest {

    private Long projectId;

    @NotNull(message = "交易日期不能为空")
    private LocalDateTime transactionDate;

    @NotNull(message = "金额不能为空")
    @DecimalMin(value = "0", message = "金额不能为负")
    private BigDecimal amount;

    @NotNull(message = "收支类型不能为空")
    private AccountType type;

    /** 票据ID（逻辑引用，不强制校验存在性）。 */
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
