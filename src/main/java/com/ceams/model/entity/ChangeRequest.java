package com.ceams.model.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class ChangeRequest {
    private Integer requestId;
    private Integer projectId;
    private String requestType;
    private String description;
    private BigDecimal estimatedCost;
    private String status;
    private Integer requestedBy;
    private Integer approvedBy;
    private Date approvedAt;
    private Integer userId;
    private Date createdAt;
    private Date updatedAt;
}