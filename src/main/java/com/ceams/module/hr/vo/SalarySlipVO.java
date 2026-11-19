package com.ceams.module.hr.vo;

import com.ceams.module.hr.enums.SalarySlipReadStatus;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/** 工资条视图对象（金额为解密后的数值，禁止写入日志）。 */
@Data
public class SalarySlipVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long employeeId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long calcId;

    private String employeeNo;

    private String employeeName;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long deptId;

    /** 核算月（yyyy-MM）。 */
    private String calcMonth;

    private BigDecimal baseSalary;

    private BigDecimal postSalary;

    private BigDecimal performanceAmount;

    private BigDecimal allowance;

    private BigDecimal attendanceDeduction;

    private BigDecimal socialInsurance;

    private BigDecimal housingFund;

    private BigDecimal tax;

    private BigDecimal grossPay;

    private BigDecimal netPay;

    /** 应出勤天数。 */
    private BigDecimal attendanceDays;

    /** 缺勤天数。 */
    private BigDecimal absentDays;

    /** 迟到 / 早退次数。 */
    private Integer lateCount;

    /** 绩效系数。 */
    private BigDecimal performanceCoefficient;

    /** 阅读状态：PENDING/READ。 */
    private SalarySlipReadStatus readStatus;

    /** 发布时间。 */
    private LocalDateTime publishedAt;

    /** 阅读时间。 */
    private LocalDateTime readAt;

    private LocalDateTime createTime;
}
