package com.ceams.module.hr.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

/** 首次建立薪资台账请求（仅员工无 ACTIVE 记录时允许）。 */
@Data
public class SalaryCreateRequest {

    @NotNull(message = "员工 ID 不能为空")
    private Long employeeId;

    @NotNull(message = "生效日期不能为空")
    private LocalDate effectiveDate;

    @NotNull(message = "基本工资不能为空")
    @DecimalMin(value = "0.00", message = "基本工资不能为负数")
    private BigDecimal baseSalary;

    @DecimalMin(value = "0.00", message = "岗位工资不能为负数")
    private BigDecimal postSalary;

    @DecimalMin(value = "0.00", message = "绩效基数不能为负数")
    private BigDecimal performanceBase;

    @DecimalMin(value = "0.00", message = "固定补贴不能为负数")
    private BigDecimal allowance;

    @Size(max = 500, message = "备注长度不能超过 500")
    private String remark;
}
