package com.ceams.module.report.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 项目成本报表视图对象：按项目维度输出预算 / 变更 / 已付 / 成本分摊对比。
 */
@Data
public class ProjectCostReportVO {

    /** 项目成本明细列表。 */
    private List<ProjectCostItemVO> items;

    /** 预算金额合计。 */
    private BigDecimal totalBudget = BigDecimal.ZERO;

    /** 已变更金额合计。 */
    private BigDecimal totalChanged = BigDecimal.ZERO;

    /** 已付进度款合计。 */
    private BigDecimal totalPaid = BigDecimal.ZERO;

    /** 成本分摊金额合计。 */
    private BigDecimal totalAllocated = BigDecimal.ZERO;
}
