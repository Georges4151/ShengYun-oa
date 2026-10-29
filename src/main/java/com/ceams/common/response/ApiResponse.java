package com.ceams.common.response;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/**
 * 统一 API 响应体。
 *
 * <pre>{@code
 * { "code": 0, "message": "success", "data": {}, "traceId": "...", "timestamp": "..." }
 * }</pre>
 *
 * @param <T> 业务数据负载类型
 */
@Getter
@Setter
public class ApiResponse<T> {

    /** 业务码：0 成功，非 0 为错误码。 */
    private int code;

    /** 消息：成功时为 success，失败时为业务码字符串（RFC7807 + 业务码前缀）。 */
    private String message;

    /** 业务数据负载。 */
    private T data;

    /** 链路追踪 ID。 */
    private String traceId;

    /** 响应时间（ISO8601）。 */
    private LocalDateTime timestamp;

    private ApiResponse() {}

    public static <T> ApiResponse<T> of(ResultCode resultCode, T data, String message) {
        ApiResponse<T> resp = new ApiResponse<>();
        resp.code = resultCode.getCode();
        resp.message = message;
        resp.data = data;
        resp.traceId = newTraceId();
        resp.timestamp = LocalDateTime.now();
        return resp;
    }

    /** 成功（data 为具体负载）。 */
    public static <T> ApiResponse<T> success(T data) {
        return of(ResultCode.SUCCESS, data, ResultCode.SUCCESS.getMessage());
    }

    /** 成功（data 为 null，如删除/更新）。 */
    public static <T> ApiResponse<T> success() {
        return success(null);
    }

    /** 失败（自定义 code/message）。 */
    public static <T> ApiResponse<T> error(int code, String message) {
        ApiResponse<T> resp = new ApiResponse<>();
        resp.code = code;
        resp.message = message;
        resp.data = null;
        resp.traceId = newTraceId();
        resp.timestamp = LocalDateTime.now();
        return resp;
    }

    private static String newTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
