package com.ceams.module.hr.vo;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/** 人力成本汇总（只读聚合视图，金额均为明文 BigDecimal）。 */
@Data
public class PayrollCostSummaryVO {

    /** 核算月（yyyy-MM）。 */
    private String month;

    /** 纳入成本口径的员工数（distinct employeeId）。 */
    private long headcount;

    /** 应发合计。 */
    private BigDecimal grossPay;

    /** 实发合计。 */
    private BigDecimal netPay;

    /** 社保合计。 */
    private BigDecimal socialInsurance;

    /** 公积金合计。 */
    private BigDecimal housingFund;

    /** 个人所得税合计。 */
    private BigDecimal tax;

    /** 人均实发（netPay / headcount）。 */
    private BigDecimal avgNetPay;

    /** 按部门分组明细（deptId 升序，未归属部门的记录排在最后）。 */
    private List<PayrollCostDeptVO> byDept = new ArrayList<>();
}
