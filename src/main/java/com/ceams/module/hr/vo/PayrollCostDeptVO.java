package com.ceams.module.hr.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.math.BigDecimal;
import lombok.Data;

/** 人力成本按部门分组视图（只读聚合，金额均为明文 BigDecimal）。 */
@Data
public class PayrollCostDeptVO {

    /** 部门 ID；未归属部门的记录为 null。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long deptId;

    /** 部门名称；部门不存在时回落空串，未归属部门为「未分配部门」。 */
    private String deptName;

    /** 部门内纳入成本口径的员工数（distinct employeeId）。 */
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

    /** 人均实发（netPay / headcount，headcount 为 0 时取 0）。 */
    private BigDecimal avgNetPay;
}
