package com.ceams.module.hr.vo;

import com.ceams.module.hr.enums.PayrollStatus;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/** 月度工资核算明细视图对象（金额为解密后的数值）。 */
@Data
public class SalaryCalcVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long employeeId;

    private String employeeNo;

    private String employeeName;

    /** 核算月（yyyy-MM）。 */
    private String calcMonth;

    private BigDecimal baseSalary;

    private BigDecimal postSalary;

    /** 应出勤天数。 */
    private BigDecimal attendanceDays;

    /** 缺勤天数。 */
    private BigDecimal absentDays;

    /** 迟到 / 早退次数。 */
    private Integer lateCount;

    private BigDecimal attendanceDeduction;

    private BigDecimal performanceCoefficient;

    private BigDecimal performanceAmount;

    private BigDecimal allowance;

    private BigDecimal socialInsurance;

    private BigDecimal housingFund;

    private BigDecimal tax;

    private BigDecimal grossPay;

    private BigDecimal netPay;

    private PayrollStatus status;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long confirmedBy;

    private LocalDateTime confirmedAt;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
