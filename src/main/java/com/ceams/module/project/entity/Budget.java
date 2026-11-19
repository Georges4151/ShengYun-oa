package com.ceams.module.project.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ceams.common.base.BaseEntity;
import com.ceams.module.project.enums.BudgetStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 预算（biz_budget）。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_budget")
public class Budget extends BaseEntity {

    /** 预算名。 */
    private String budgetName;

    /** 预算金额。 */
    private BigDecimal amount;

    /** 项目 ID（逻辑引用 biz_project.id）。 */
    private Long projectId;

    /** 分包合同 ID（逻辑引用 biz_subcontractor_contract.id）。 */
    private Long contractId;

    /** 开始日期。 */
    private LocalDateTime startDate;

    /** 结束日期。 */
    private LocalDateTime endDate;

    /** 预算状态。 */
    private BudgetStatus status;

    /** 已用金额。 */
    private BigDecimal usedAmount = BigDecimal.ZERO;

    /** 剩余金额。 */
    private BigDecimal remainingAmount;
}
