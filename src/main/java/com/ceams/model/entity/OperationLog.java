package com.ceams.model.entity;

import lombok.Data;

import java.util.Date;

@Data
public class OperationLog {
    private Integer logId;
    private Integer userId;
    private String operationType;
    private String operationDescription;
    private Integer targetId;
    private String targetType;
    private String ipAddress;
    private Date createdAt;
}