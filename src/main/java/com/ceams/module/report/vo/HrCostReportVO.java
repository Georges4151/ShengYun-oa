package com.ceams.module.report.vo;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/** 人力成本报表视图对象：总额 + 按部门明细。 */
@Data
public class HrCostReportVO {

    /** 核算月（yyyy-MM）。 */
    private String month;

    /** 在职人数（Σ(totalCount - terminatedCount)，含试用、含休假）。 */
    private long headcount;

    /** 工资口径人数（当月已确认 / 已锁定核算记录的员工数）。 */
    private long payrollHeadcount;

    /** 应发合计。 */
    private BigDecimal grossPay = BigDecimal.ZERO;

    /** 实发合计。 */
    private BigDecimal netPay = BigDecimal.ZERO;

    /** 社保合计。 */
    private BigDecimal socialInsurance = BigDecimal.ZERO;

    /** 公积金合计。 */
    private BigDecimal housingFund = BigDecimal.ZERO;

    /** 个人所得税合计。 */
    private BigDecimal tax = BigDecimal.ZERO;

    /** 人均实发。 */
    private BigDecimal avgNetPay = BigDecimal.ZERO;

    /** 按部门明细（deptId 升序，未分配部门排在最后）。 */
    private List<HrCostDeptVO> byDept = new ArrayList<>();
}
