package com.ceams.module.payment.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ceams.common.base.BaseEntity;
import com.ceams.module.payment.enums.AccountType;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 付款交易（biz_payment_transaction）。无 status 字段。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_payment_transaction")
public class PaymentTransaction extends BaseEntity {

    /** 付款账户ID（逻辑引用 biz_payment_account.id）。 */
    private Long accountId;

    /** 票据ID（逻辑引用 biz_receipt.id）。 */
    private Long receiptId;

    /** 项目ID（逻辑引用 biz_project.id）。 */
    private Long projectId;

    /** 金额。 */
    private BigDecimal amount;

    /** 类型：INCOME/EXPENSE。 */
    private AccountType type;

    /** 交易时间。 */
    private LocalDateTime transactionDate;

    /** 描述。 */
    private String description;
}
