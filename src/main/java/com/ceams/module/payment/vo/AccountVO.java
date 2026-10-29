package com.ceams.module.payment.vo;

import com.ceams.module.payment.enums.AccountStatus;
import com.ceams.module.payment.enums.AccountType;
import com.ceams.module.payment.enums.AllocationStatus;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/** 收支流水视图对象。 */
@Data
public class AccountVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long projectId;

    private LocalDateTime transactionDate;

    private BigDecimal amount;

    private AccountType type;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long receiptId;

    private String description;

    private AccountStatus status;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long paymentAccountId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long contractId;

    private String paymentType;

    private AllocationStatus allocationStatus;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long categoryId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long contactId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long materialId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long subcontractorId;

    private LocalDateTime createTime;
}
