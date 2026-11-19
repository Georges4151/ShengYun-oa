package com.ceams.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * RFC7807 错误码枚举。
 *
 * <p>业务码前缀：AUTH_（认证/授权）、VALID_（参数校验）、BIZ_（业务规则）、SIGN_（签名）。 数值 code 按 HTTP 语义分段：400xxx 校验、401/403xxx
 * 认证授权、404/409/422xxx 业务、500xxx 系统。
 */
@Getter
public enum ErrorCode {

    // ---------- AUTH 认证/授权 ----------
    AUTH_INVALID_CREDENTIALS(401_001, HttpStatus.UNAUTHORIZED, "账号或密码错误"),
    AUTH_TOKEN_EXPIRED(401_002, HttpStatus.UNAUTHORIZED, "access token 已过期或无效"),
    AUTH_TOKEN_INVALID(401_003, HttpStatus.UNAUTHORIZED, "无效令牌"),
    AUTH_REFRESH_REVOKED(401_004, HttpStatus.UNAUTHORIZED, "refresh token 已吊销"),
    AUTH_ACCESS_DENIED(403_001, HttpStatus.FORBIDDEN, "无权访问该资源"),

    // ---------- VALID 参数校验 ----------
    VALID_REQUIRED_FIELD(400_001, HttpStatus.BAD_REQUEST, "缺少必填字段"),
    VALID_ILLEGAL_ARGUMENT(400_002, HttpStatus.BAD_REQUEST, "参数非法"),
    VALID_MALFORMED_JSON(400_003, HttpStatus.BAD_REQUEST, "请求体 JSON 格式错误"),
    VALID_PAGE_BOUNDS(400_004, HttpStatus.BAD_REQUEST, "分页参数越界"),

    // ---------- BIZ 业务规则 ----------
    BIZ_NOT_FOUND(404_001, HttpStatus.NOT_FOUND, "资源不存在"),
    BIZ_DUPLICATE_CODE(409_001, HttpStatus.CONFLICT, "编码或名称已存在"),
    BIZ_STATE_CONFLICT(409_002, HttpStatus.CONFLICT, "当前状态不允许该操作"),
    BIZ_REFERENCED(409_003, HttpStatus.CONFLICT, "存在关联数据，无法删除"),
    BIZ_UNPROCESSABLE(422_001, HttpStatus.UNPROCESSABLE_ENTITY, "业务处理失败"),

    // ---------- BPM 流程内核 ----------
    BPM_SCHEMA_INVALID(400_010, HttpStatus.BAD_REQUEST, "流程模板 JSON 非法"),
    BPM_TASK_NOT_OWNED(403_012, HttpStatus.FORBIDDEN, "非本人待办，无权操作"),
    BPM_TEMPLATE_NOT_ENABLED(409_010, HttpStatus.CONFLICT, "BPM 模板未启用"),
    BPM_TASK_CONFLICT(409_011, HttpStatus.CONFLICT, "任务已被处理或版本冲突，请刷新后重试"),
    BPM_INSTANCE_CONFLICT(409_012, HttpStatus.CONFLICT, "流程实例状态或版本冲突，请刷新后重试"),
    BPM_ASSIGNEE_EMPTY(422_010, HttpStatus.UNPROCESSABLE_ENTITY, "审批人解析为空"),

    // ---------- SIGN 签名 ----------
    SIGN_TIMESTAMP_EXPIRED(400_005, HttpStatus.BAD_REQUEST, "签名时间戳超出 ±5min 窗口"),
    SIGN_NONCE_REPLAY(400_006, HttpStatus.BAD_REQUEST, "nonce 重复（防重放拦截）"),
    SIGN_INVALID(400_007, HttpStatus.BAD_REQUEST, "HMAC 签名校验失败"),

    // ---------- 系统 ----------
    SYSTEM_INTERNAL_ERROR(500_001, HttpStatus.INTERNAL_SERVER_ERROR, "系统内部错误"),
    /** 依赖的外部服务不可用（如 OnlyOffice Document Server 未启用或探活失败）。 */
    SYSTEM_SERVICE_UNAVAILABLE(503_001, HttpStatus.SERVICE_UNAVAILABLE, "依赖服务暂不可用"),
    SYSTEM_RATE_LIMITED(429_001, HttpStatus.TOO_MANY_REQUESTS, "请求过于频繁");

    /** 数值业务码（响应体 code）。 */
    private final int code;

    /** RFC7807 对应 HTTP 状态码。 */
    private final HttpStatus httpStatus;

    /** 默认人类可读描述。 */
    private final String description;

    ErrorCode(int code, HttpStatus httpStatus, String description) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.description = description;
    }
}
