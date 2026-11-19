package com.ceams.common.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** 客户端 IP 解析工具。 */
public final class ClientIpResolver {

    private static final String FORWARDED_FOR = "X-Forwarded-For";

    private ClientIpResolver() {}

    /**
     * 解析请求来源 IP：优先取 {@code X-Forwarded-For} 的首个地址，否则回落到 {@code remoteAddr}。
     *
     * @param request 当前 HTTP 请求，允许为 null
     * @return 客户端 IP，无法解析时返回 null
     */
    public static String resolve(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String forwarded = request.getHeader(FORWARDED_FOR);
        if (forwarded != null && !forwarded.isBlank()) {
            String first = forwarded.split(",")[0].trim();
            if (!first.isEmpty()) {
                return first;
            }
        }
        return request.getRemoteAddr();
    }

    /** 解析当前线程绑定请求的来源 IP；非 Web 线程（如定时任务）返回 null，不抛异常。 */
    public static String resolveCurrentRequest() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletAttributes) {
            return resolve(servletAttributes.getRequest());
        }
        return null;
    }
}
