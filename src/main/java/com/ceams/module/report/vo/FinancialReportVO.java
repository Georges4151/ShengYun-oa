package com.ceams.module.report.vo;

import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

/** 财务报表视图对象：聚合收支流水 / 付款交易 / 银行流水 / 总账。 */
@Data
public class FinancialReportVO {

    /** 合计收入。 */
    private BigDecimal totalIncome = BigDecimal.ZERO;

    /** 合计支出。 */
    private BigDecimal totalExpense = BigDecimal.ZERO;

    /** 净额（收入 - 支出）。 */
    private BigDecimal netAmount = BigDecimal.ZERO;

    /** 明细列表。 */
    private List<FinancialReportItemVO> items;
}
