package com.ceams.model.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class ProgressPayment {
    private Integer paymentId;
    private Integer projectId;
    private String paymentPeriod;
    private BigDecimal applicationAmount;
    private BigDecimal approvedAmount;
    private BigDecimal paymentAmount;
    private Date applicationDate;
    private Date approvalDate;
    private Date paymentDate;
    private String status;
    private Integer applicationBy;
    private Integer approvedBy;
    private Date createdAt;
    private Date updatedAt;
}