package com.ceams.module.hr.vo;

import com.ceams.module.hr.enums.SalaryStatus;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

/** 薪资台账视图对象（金额为解密后的数值）。 */
@Data
public class SalaryVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long employeeId;

    private String employeeNo;

    private String employeeName;

    private LocalDate effectiveDate;

    private BigDecimal baseSalary;

    private BigDecimal postSalary;

    private BigDecimal performanceBase;

    private BigDecimal allowance;

    private SalaryStatus status;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long changeApplyId;

    private String remark;

    private LocalDateTime createTime;
}
