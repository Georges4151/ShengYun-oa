package com.ceams.common.response;

import lombok.Getter;

/**
 * 统一响应码。
 */
@Getter
public enum ResultCode {

    /** 成功。 */
    SUCCESS(0, "success"),

    /** 通用系统错误兜底。 */
    ERROR(500000, "SYSTEM_INTERNAL_ERROR");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}