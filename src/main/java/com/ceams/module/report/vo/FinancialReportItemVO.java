package com.ceams.module.report.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 财务报表明细项：四类流水按统一口径合并后的单条记录。
 */
@Data
public class FinancialReportItemVO {

    /** 源记录 ID（雪花 ID，字符串化）。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 来源类型：ACCOUNT / PAYMENT_TRANSACTION / BANK_STATEMENT / GENERAL_LEDGER。 */
    private String sourceType;

    /** 项目 ID（逻辑引用 biz_project.id）。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long projectId;

    /** 项目名（冗余）。 */
    private String projectName;

    /** 账户 ID（逻辑引用 biz_payment_account.id）。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long accountId;

    /** 账户名。 */
    private String accountName;

    /** 交易时间。 */
    private LocalDateTime transactionDate;

    /** 收支类型：INCOME / EXPENSE。 */
    private String type;

    /** 金额。 */
    private BigDecimal amount;

    /** 描述 / 摘要。 */
    private String description;
}
