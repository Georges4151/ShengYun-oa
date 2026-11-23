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

    /** 项目名称（关联 biz_project.project_name）。 */
    private String projectName;

    /** 付款账户名称（关联 biz_payment_account.name）。 */
    private String paymentAccountName;

    /** 票据编号（关联 biz_receipt.receipt_number）。 */
    private String receiptNumber;

    /** 分包合同名称（关联 biz_subcontractor_contract.contract_name）。 */
    private String contractName;

    private LocalDateTime createTime;
}
