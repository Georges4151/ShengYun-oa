package com.ceams.module.bank.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ceams.common.base.BaseEntity;
import com.ceams.module.bank.enums.BankStatementStatus;
import com.ceams.module.bank.enums.TransactionType;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 银行流水（biz_bank_statement）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_bank_statement")
public class BankStatement extends BaseEntity {

    /** 付款账户ID（逻辑引用 biz_payment_account.id）。 */
    private Long accountId;

    /** 交易日期。 */
    private LocalDateTime transactionDate;

    /** 金额。 */
    private BigDecimal amount;

    /** 类型：INCOME/EXPENSE。 */
    private TransactionType type;

    /** 描述。 */
    private String description;

    /** 对方户名。 */
    private String counterparty;

    /** 银行流水号。 */
    private String bankReference;

    /** 余额。 */
    private BigDecimal balance;

    /** 对账状态：UNRECONCILED/RECONCILED/MATCHED。 */
    private BankStatementStatus status = BankStatementStatus.UNRECONCILED;
}
