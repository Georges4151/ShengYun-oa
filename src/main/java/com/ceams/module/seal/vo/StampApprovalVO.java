package com.ceams.module.seal.vo;

import com.ceams.module.seal.enums.ApprovalStatus;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.time.LocalDateTime;
import lombok.Data;

/** 用印审批视图对象。 */
@Data
public class StampApprovalVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long stampId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long sealId;

    private String sealName;

    private String sealCode;

    private String sealPermission;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long projectId;

    private String projectName;

    private String contractName;

    private String contractPath;

    private String originalContractPath;

    private String stampPositions;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long applicantId;

    private String applicantName;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long approverId;

    private String approverName;

    private ApprovalStatus status;

    private String rejectReason;

    private LocalDateTime applyTime;

    private LocalDateTime approvalTime;

    private LocalDateTime createTime;
}
