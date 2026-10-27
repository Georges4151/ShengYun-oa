package com.ceams.module.project.vo;

import com.ceams.module.project.enums.ApprovalStatus;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 变更请求视图对象。
 */
@Data
public class ChangeRequestVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long projectId;

    private String requestType;

    private String description;

    private BigDecimal estimatedCost;

    private ApprovalStatus status;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long requestedBy;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long approvedBy;

    private LocalDateTime approvedAt;

    private LocalDateTime createTime;
}