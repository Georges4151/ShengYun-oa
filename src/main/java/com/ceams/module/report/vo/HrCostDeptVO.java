package com.ceams.module.report.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.math.BigDecimal;
import lombok.Data;

/** 人力成本报表：单部门成本行。 */
@Data
public class HrCostDeptVO {

    /** 部门 ID；未分配部门的行返回 null。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long deptId;

    /** 部门名称（优先取员工口径的名称）。 */
    private String deptName;

    /** 在职人数（totalCount - terminatedCount，含试用与休假）。 */
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

    /** 人均实发（netPay / payrollHeadcount，人数为 0 时取 0）。 */
    private BigDecimal avgNetPay = BigDecimal.ZERO;
}
