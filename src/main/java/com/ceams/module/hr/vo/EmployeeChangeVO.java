package com.ceams.module.hr.vo;

import com.ceams.module.hr.enums.ChangeType;
import com.ceams.module.hr.enums.HrApprovalStatus;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

/** 入转调离申请单视图对象。 */
@Data
public class EmployeeChangeVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long employeeId;

    private ChangeType changeType;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long targetDeptId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long targetPositionId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long targetLevelId;

    /** 晋升目标职级名称。 */
    private String targetLevelName;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long resignLeaderId;

    /** 离职下属转交上级姓名。 */
    private String resignLeaderName;

    private LocalDate effectiveDate;

    private String reason;

    private HrApprovalStatus approvalStatus;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long bpmInstanceId;

    private String remark;

    private LocalDateTime createTime;
}
