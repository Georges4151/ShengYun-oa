package com.ceams.module.seal.vo;

import com.ceams.module.seal.enums.ApplicationStatus;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.time.LocalDateTime;
import lombok.Data;

/** 用印申请视图对象。 */
@Data
public class ElectronicSealApplicationVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long sealId;

    private String sealName;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long applicantId;

    private String applicantName;

    private String purpose;

    private ApplicationStatus status;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long projectId;

    private String projectName;

    private String contractName;

    private String contractPath;

    private String stampPosition;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long approverId;

    private String approverName;

    private String approvalComment;

    private LocalDateTime applyTime;

    private LocalDateTime approvalTime;

    private LocalDateTime createTime;
}
