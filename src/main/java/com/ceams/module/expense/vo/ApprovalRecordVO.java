package com.ceams.module.expense.vo;

import com.ceams.module.expense.enums.ApprovalRecordStatus;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.time.LocalDateTime;
import lombok.Data;

/** 审批记录视图对象。 */
@Data
public class ApprovalRecordVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long receiptId;

    private Integer approvalLevel;

    private String approverRole;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long approverId;

    private String approverName;

    private ApprovalRecordStatus status;

    private String approvalComment;

    private LocalDateTime approvalTime;

    private LocalDateTime createTime;
}
