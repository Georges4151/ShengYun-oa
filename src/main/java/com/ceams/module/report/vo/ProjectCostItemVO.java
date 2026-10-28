package com.ceams.module.report.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 项目成本报表明细项。
 */
@Data
public class ProjectCostItemVO {

    /** 项目 ID（逻辑引用 biz_project.id）。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long projectId;

    /** 项目名。 */
    private String projectName;

    /** 合同金额（biz_project.contract_amount）。 */
    private BigDecimal contractAmount;

    /** 预算金额（biz_budget.amount 汇总）。 */
    private BigDecimal budgetAmount;

    /** 已变更金额（biz_change_request.estimated_cost，仅 APPROVED 汇总）。 */
    private BigDecimal changedAmount;

    /** 已付进度款（biz_progress_payment.payment_amount 汇总）。 */
    private BigDecimal paidAmount;

    /** 成本分摊金额（biz_cost_allocation.allocation_amount，PROJECT 维度汇总）。 */
    private BigDecimal allocatedAmount;

    /** 项目台账实际已发生金额（biz_project.actual_amount）。 */
    private BigDecimal actualAmount;
}
