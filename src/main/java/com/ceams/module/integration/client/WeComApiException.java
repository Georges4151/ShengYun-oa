package com.ceams.module.integration.client;

import lombok.Getter;

/**
 * 企业微信 API 调用异常。
 *
 * <p>{@code errcode} 为企业微信返回的错误码；网络异常或响应解析失败统一使用 {@code -1}。
 */
@Getter
public class WeComApiException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** 企业微信错误码；-1 表示本地网络 / 解析异常。 */
    private final int errcode;

    /** 企业微信错误信息。 */
    private final String errmsg;

    public WeComApiException(int errcode, String errmsg) {
        super(errmsg);
        this.errcode = errcode;
        this.errmsg = errmsg;
    }

    public WeComApiException(int errcode, String errmsg, Throwable cause) {
        super(errmsg, cause);
        this.errcode = errcode;
        this.errmsg = errmsg;
    }
}
