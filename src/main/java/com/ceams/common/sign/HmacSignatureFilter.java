package com.ceams.common.sign;

import com.ceams.common.exception.ErrorCode;
import com.ceams.common.response.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * HMAC 签名校验过滤器：仅对命中 {@link HmacSigned} 的处理器生效。
 *
 * <p>校验流程（顺序固定）：
 * <ol>
 *   <li>根据请求映射定位 handler，仅方法/类标注 {@link HmacSigned} 时执行，其余直接放行。</li>
 *   <li>读取 {@code X-Timestamp}，校验落在 ±window 时间窗内，否则 {@code SIGN_TIMESTAMP_EXPIRED}。</li>
 *   <li>通过 {@link NonceStore} 占用 {@code X-Nonce}，重复则 {@code SIGN_NONCE_REPLAY}。</li>
 *   <li>重算 {@code HMAC-SHA256(timestamp + nonce + bodyHash)} 并与 {@code X-Sign} 常量时间比较，
 *       不一致则 {@code SIGN_INVALID}。</li>
 * </ol>
 * </p>
 *
 * <p>bodyHash 基于「已序列化的原始请求体字节」的 SHA-256 hex；空请求体时 bodyHash 为空串 {@code ""}，
 * 与前端 {@code sign.ts} 约定一致。过滤器需在控制器消费请求体前完成校验，因此这里使用可重复读取的
 * 请求体包装器缓存原始字节（等价于 Spring {@code ContentCachingRequestWrapper} 的缓存语义，
 * 同时保证下游控制器仍能再次读取请求体）。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HmacSignatureFilter extends OncePerRequestFilter {

    private final RequestMappingHandlerMapping handlerMapping;
    private final SignSecretResolver secretResolver;
    private final NonceStore nonceStore;
    private final ObjectMapper objectMapper;

    /** 签名时间戳允许偏差（毫秒），默认 ±5 分钟。 */
    @Value("${ceams.sign.window-millis:300000}")
    private long windowMillis;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!isHmacSigned(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 缓存原始请求体，供 bodyHash 计算，同时保证下游可重复读取
        CachedBodyRequestWrapper cachedRequest = new CachedBodyRequestWrapper(request);
        byte[] body = cachedRequest.getBody();
        String bodyHash = body.length == 0 ? "" : HmacSignUtil.sha256Hex(new String(body, StandardCharsets.UTF_8));

        String timestamp = request.getHeader("X-Timestamp");
        String nonce = request.getHeader("X-Nonce");
        String sign = request.getHeader("X-Sign");
        if (StringUtils.isAnyBlank(timestamp, nonce, sign)) {
            writeError(response, ErrorCode.SIGN_INVALID);
            return;
        }

        long timestampMillis;
        try {
            timestampMillis = Long.parseLong(timestamp);
        } catch (NumberFormatException ex) {
            writeError(response, ErrorCode.SIGN_INVALID);
            return;
        }

        if (!HmacSignUtil.withinWindow(timestampMillis, System.currentTimeMillis(), windowMillis)) {
            writeError(response, ErrorCode.SIGN_TIMESTAMP_EXPIRED);
            return;
        }

        // nonce 防重放：异常时快速失败，不放行
        boolean acquired;
        try {
            acquired = nonceStore.tryAcquire(nonce, windowMillis);
        } catch (Exception ex) {
            log.error("nonce 防重放存储异常，快速失败不放行", ex);
            writeError(response, ErrorCode.SYSTEM_INTERNAL_ERROR);
            return;
        }
        if (!acquired) {
            writeError(response, ErrorCode.SIGN_NONCE_REPLAY);
            return;
        }

        String expected = HmacSignUtil.sign(timestamp, nonce, bodyHash, secretResolver.resolve());
        if (!HmacSignUtil.constantTimeEquals(expected, sign)) {
            writeError(response, ErrorCode.SIGN_INVALID);
            return;
        }

        filterChain.doFilter(cachedRequest, response);
    }

    /** 依据 request 定位 handler，判断方法或类是否标注 {@link HmacSigned}。 */
    private boolean isHmacSigned(HttpServletRequest request) {
        try {
            HandlerExecutionChain chain = handlerMapping.getHandler(request);
            if (chain == null) {
                return false;
            }
            Object handler = chain.getHandler();
            if (handler instanceof HandlerMethod handlerMethod) {
                return handlerMethod.hasMethodAnnotation(HmacSigned.class)
                        || handlerMethod.getBeanType().isAnnotationPresent(HmacSigned.class);
            }
            return false;
        } catch (Exception ex) {
            return false;
        }
    }

    /** 过滤器内直接写统一错误响应（@RestControllerAdvice 无法捕获过滤器异常，与 SecurityConfig 写法一致）。 */
    private void writeError(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setStatus(errorCode.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        ApiResponse<Void> body = ApiResponse.error(errorCode.getCode(), errorCode.name());
        objectMapper.writeValue(response.getWriter(), body);
    }

    /**
     * 可重复读取的请求体包装器：构造时一次性读取并缓存原始字节，
     * 之后通过 {@link #getInputStream()} / {@link #getReader()} 重复提供。
     */
    private static final class CachedBodyRequestWrapper extends HttpServletRequestWrapper {

        private final byte[] body;

        CachedBodyRequestWrapper(HttpServletRequest request) throws IOException {
            super(request);
            this.body = request.getInputStream().readAllBytes();
        }

        byte[] getBody() {
            return body;
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream bis = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                @Override
                public boolean isFinished() {
                    return bis.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener readListener) {
                    // 同步读取实现，无需异步回调
                }

                @Override
                public int read() {
                    return bis.read();
                }
            };
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }
    }
}
