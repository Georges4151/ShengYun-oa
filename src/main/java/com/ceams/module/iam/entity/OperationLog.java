package com.ceams.module.iam.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ceams.common.base.BaseEntity;
import com.ceams.module.iam.enums.OperationType;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 操作日志（sys_operation_log）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_operation_log")
public class OperationLog extends BaseEntity {

    /** 操作人 ID。 */
    private Long userId;

    /** 操作人账号（冗余）。 */
    private String username;

    /** 操作类型。 */
    private OperationType operationType;

    /** 操作描述。 */
    private String operationDescription;

    /** 目标类型。 */
    private String targetType;

    /** 目标 ID。 */
    private Long targetId;

    /** IP 地址。 */
    private String ipAddress;

    /** 浏览器 UA。 */
    private String userAgent;

    /** HTTP 方法。 */
    private String requestMethod;

    /** 请求 URI。 */
    private String requestUri;

    /** 执行结果：SUCCESS/FAILED。 */
    private String status;

    /** 异常信息。 */
    private String errorMessage;
}