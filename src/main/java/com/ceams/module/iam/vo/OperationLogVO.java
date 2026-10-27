package com.ceams.module.iam.vo;

import com.ceams.module.iam.enums.OperationType;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志视图对象。
 */
@Data
public class OperationLogVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    private String username;

    private OperationType operationType;

    private String operationDescription;

    private String targetType;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long targetId;

    private String ipAddress;

    private LocalDateTime createTime;
}