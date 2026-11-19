package com.ceams.module.hr.vo;

import com.ceams.module.hr.enums.CheckinType;
import com.ceams.module.hr.enums.HrApprovalStatus;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

/** 补卡申请单视图对象。 */
@Data
public class CheckinSupplementVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long employeeId;

    private LocalDate checkinDate;

    private LocalDateTime checkinTime;

    private CheckinType checkinType;

    private String reason;

    private HrApprovalStatus approvalStatus;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long bpmInstanceId;

    private LocalDateTime createTime;
}
