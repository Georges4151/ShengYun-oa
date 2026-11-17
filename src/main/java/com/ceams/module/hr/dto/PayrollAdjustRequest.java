package com.ceams.module.hr.dto;

import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;
import lombok.Data;

/**
 * 月度工资核算手工调整请求（仅 DRAFT 允许）。
 *
 * <p>各字段均可空：为空表示保持原值；至少填写一项，调整后由服务端重新计算应发与实发合计。
 */
@Data
public class PayrollAdjustRequest {

    /** 补贴（可空）。 */
    @DecimalMin(value = "0.00", message = "补贴不能为负数")
    private BigDecimal allowance;

    /** 考勤扣款（可空）。 */
    @DecimalMin(value = "0.00", message = "考勤扣款不能为负数")
    private BigDecimal attendanceDeduction;

    /** 绩效系数（可空）。 */
    @DecimalMin(value = "0.0000", message = "绩效系数不能为负数")
    private BigDecimal performanceCoefficient;

    /** 社保（可空）。 */
    @DecimalMin(value = "0.00", message = "社保不能为负数")
    private BigDecimal socialInsurance;

    /** 公积金（可空）。 */
    @DecimalMin(value = "0.00", message = "公积金不能为负数")
    private BigDecimal housingFund;

    /** 个人所得税（可空）。 */
    @DecimalMin(value = "0.00", message = "个人所得税不能为负数")
    private BigDecimal tax;
}
