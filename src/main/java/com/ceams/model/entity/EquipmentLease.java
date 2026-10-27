package com.ceams.model.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class EquipmentLease {
    private Integer leaseId;
    private Integer equipmentId;
    private Integer projectId;
    private Integer lessorId;
    private Date leaseStartDate;
    private Date leaseEndDate;
    private BigDecimal dailyRent;
    private BigDecimal totalRent;
    private String paymentStatus;
    private Date createdAt;
    private Date updatedAt;
}