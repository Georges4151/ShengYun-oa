package com.ceams.module.hr.dto;

import com.ceams.module.hr.enums.SalaryChangeType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

/** 发起薪资变动申请（提交即启动 BPM）。 */
@Data
public class SalaryChangeApplyRequest {

    @NotNull(message = "员工 ID 不能为空")
    private Long employeeId;

    @NotNull(message = "变动类型不能为空")
    private SalaryChangeType changeType;

    @NotNull(message = "生效日期不能为空")
    private LocalDate effectiveDate;

    /** 为空表示沿用当前生效台账的对应金额。 */
    @DecimalMin(value = "0.00", message = "基本工资不能为负数")
    private BigDecimal baseSalary;

    @DecimalMin(value = "0.00", message = "岗位工资不能为负数")
    private BigDecimal postSalary;

    @DecimalMin(value = "0.00", message = "绩效基数不能为负数")
    private BigDecimal performanceBase;

    @DecimalMin(value = "0.00", message = "固定补贴不能为负数")
    private BigDecimal allowance;

    @Size(max = 500, message = "申请原因长度不能超过 500")
    private String reason;
}
