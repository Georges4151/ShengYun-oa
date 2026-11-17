package com.ceams.module.hr.vo;

import com.ceams.module.hr.enums.HrApprovalStatus;
import com.ceams.module.hr.enums.LeaveType;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

/** 请假申请单视图对象。 */
@Data
public class LeaveRequestVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long employeeId;

    private LeaveType leaveType;

    private LocalDate startDate;

    private LocalDate endDate;

    private BigDecimal days;

    private String reason;

    private HrApprovalStatus approvalStatus;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long bpmInstanceId;

    private LocalDateTime createTime;
}
