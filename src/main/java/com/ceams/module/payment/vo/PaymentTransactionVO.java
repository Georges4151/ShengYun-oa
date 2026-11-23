package com.ceams.module.payment.vo;

import com.ceams.module.payment.enums.AccountType;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/** 付款交易视图对象。 */
@Data
public class PaymentTransactionVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long accountId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long receiptId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long projectId;

    private BigDecimal amount;

    private AccountType type;

    private LocalDateTime transactionDate;

    private String description;

    /** 付款账户名称（关联 biz_payment_account.name）。 */
    private String accountName;

    /** 票据编号（关联 biz_receipt.receipt_number）。 */
    private String receiptNumber;

    /** 项目名称（关联 biz_project.project_name）。 */
    private String projectName;

    private LocalDateTime createTime;
}
