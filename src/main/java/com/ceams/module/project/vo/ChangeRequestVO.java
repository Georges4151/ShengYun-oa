package com.ceams.module.project.vo;

import com.ceams.module.project.enums.ApprovalStatus;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/** 变更请求视图对象。 */
@Data
public class ChangeRequestVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long projectId;

    /** 项目名称（列表/详情展示用，非持久化字段）。 */
    private String projectName;

    private String requestType;

    private String description;

    private BigDecimal estimatedCost;

    private ApprovalStatus status;

    /** 关联的 BPM 流程实例 ID（审批状态的唯一来源）。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long bpmInstanceId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long requestedBy;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long approvedBy;

    private LocalDateTime approvedAt;

    private String approvalComment;

    private LocalDateTime createTime;
}
