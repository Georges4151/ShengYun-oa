package com.ceams.module.bank.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ceams.common.base.BaseEntity;
import com.ceams.module.bank.enums.DiscrepancyType;
import com.ceams.module.bank.enums.ReconciliationStatus;
import com.ceams.module.bank.enums.ResolutionStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 对账结果（biz_reconciliation_result）。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_reconciliation_result")
public class ReconciliationResult extends BaseEntity {

    /** 付款账户ID（逻辑引用 biz_payment_account.id）。 */
    private Long accountId;

    /** 银行流水ID（逻辑引用 biz_bank_statement.id）。 */
    private Long statementId;

    /** 付款交易ID（逻辑引用 biz_payment_transaction.id）。 */
    private Long transactionId;

    /** 对账状态：MATCHED/UNMATCHED/PARTIAL_MATCHED。 */
    private ReconciliationStatus status;

    /** 差异类型。 */
    private DiscrepancyType discrepancyType;

    /** 银行金额。 */
    private BigDecimal bankAmount;

    /** 系统金额。 */
    private BigDecimal systemAmount;

    /** 银行交易日期。 */
    private LocalDateTime bankDate;

    /** 系统交易日期。 */
    private LocalDateTime systemDate;

    /** 银行描述。 */
    private String bankDescription;

    /** 系统描述。 */
    private String systemDescription;

    /** 银行流水参考号。 */
    private String statementReference;

    /** 系统交易参考号。 */
    private String transactionReference;

    /** 处理状态：UNRESOLVED/RESOLVED。 */
    private ResolutionStatus resolutionStatus = ResolutionStatus.UNRESOLVED;

    /** 处理说明。 */
    private String resolutionNotes;

    /** 对账时间。 */
    private LocalDateTime reconciliationDate;
}
