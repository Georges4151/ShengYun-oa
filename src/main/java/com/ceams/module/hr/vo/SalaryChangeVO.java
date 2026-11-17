package com.ceams.module.hr.vo;

import com.ceams.module.hr.enums.SalaryChangeStatus;
import com.ceams.module.hr.enums.SalaryChangeType;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

/** 薪资变动申请视图对象（变更前后摘要为解密后的可读文本）。 */
@Data
public class SalaryChangeVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long employeeId;

    private String employeeNo;

    private String employeeName;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long applicantId;

    private String applicantName;

    private SalaryChangeType changeType;

    private LocalDate effectiveDate;

    /** 变更前摘要（首次建档为空）。 */
    private String beforeSummary;

    /** 变更后摘要。 */
    private String afterSummary;

    private String reason;

    private SalaryChangeStatus status;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long bpmInstanceId;

    private LocalDateTime createTime;
}
