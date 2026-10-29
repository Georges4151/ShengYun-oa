package com.ceams.module.payment.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ceams.common.base.BaseEntity;
import com.ceams.module.payment.enums.AccountStatus;
import com.ceams.module.payment.enums.AccountType;
import com.ceams.module.payment.enums.AllocationStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 收支流水（biz_account）。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_account")
public class Account extends BaseEntity {

    /** 项目ID（逻辑引用 biz_project.id）。 */
    private Long projectId;

    /** 交易日期。 */
    private LocalDateTime transactionDate;

    /** 金额。 */
    private BigDecimal amount;

    /** 类型：INCOME/EXPENSE。 */
    private AccountType type;

    /** 票据ID（逻辑引用 biz_receipt.id）。 */
    private Long receiptId;

    /** 描述。 */
    private String description;

    /** 状态：DRAFT/CONFIRMED/CANCELLED。 */
    private AccountStatus status;

    /** 付款账户ID（逻辑引用 biz_payment_account.id）。 */
    private Long paymentAccountId;

    /** 分包合同ID（逻辑引用 biz_subcontractor_contract.id）。 */
    private Long contractId;

    /** 支付类型（代码值）。 */
    private String paymentType;

    /** 成本分摊状态：UNALLOCATED/PARTIAL/ALLOCATED。 */
    private AllocationStatus allocationStatus;

    /** 分类ID（逻辑引用 biz_category.id）。 */
    private Long categoryId;

    /** 往来单位ID（逻辑引用 biz_contact.id）。 */
    private Long contactId;

    /** 材料ID（逻辑引用 biz_material.id）。 */
    private Long materialId;

    /** 分包商ID（逻辑引用 biz_subcontractor.id）。 */
    private Long subcontractorId;
}
