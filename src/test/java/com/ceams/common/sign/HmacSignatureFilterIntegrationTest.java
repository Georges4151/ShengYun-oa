package com.ceams.common.sign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ceams.common.exception.ErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * {@link HmacSignatureFilter} 链路级测试（不启动 Spring 容器）。
 *
 * <p>通过手工构造过滤器 + Mock 请求/响应 + Mock {@link RequestMappingHandlerMapping}，覆盖：合法签名放行、必填头缺失、
 * 时间戳过期、nonce 重放、请求体篡改、未标注 {@link HmacSigned} 的处理器直接放行。
 */
class HmacSignatureFilterIntegrationTest {

    /** 测试用设备 AppSecret。 */
    private static final String DEVICE_SECRET = "test-device-app-secret";

    /** 签名时间窗口（毫秒），与生产默认值一致。 */
    private static final long WINDOW_MILLIS = 300_000L;

    /** 请求路径（仅用于构造请求，不参与校验逻辑）。 */
    private static final String REQUEST_PATH = "/api/seal/stamp";

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private RequestMappingHandlerMapping handlerMapping;
    private SignSecretResolver secretResolver;
    private NonceStore nonceStore;
    private HmacSignatureFilter filter;
    private SignTestController controller;

    @BeforeEach
    void setUp() throws Exception {
        handlerMapping = mock(RequestMappingHandlerMapping.class);
        secretResolver = mock(SignSecretResolver.class);
        nonceStore = mock(NonceStore.class);
        filter = new HmacSignatureFilter(handlerMapping, secretResolver, nonceStore, objectMapper);
        // 手工注入时间窗，避免依赖 Spring 环境
        ReflectionTestUtils.setField(filter, "windowMillis", WINDOW_MILLIS);

        controller = new SignTestController();
        // 默认命中标注了 @HmacSigned 的处理器
        when(handlerMapping.getHandler(any()))
                .thenReturn(new HandlerExecutionChain(
                        new HandlerMethod(controller, controller.signedMethod())));
        when(secretResolver.resolve(any())).thenReturn(DEVICE_SECRET);
        when(nonceStore.tryAcquire(anyString(), anyLong())).thenReturn(true);
    }

    @Test
    void validSignatureIsPassedToFilterChain() throws Exception {
        String body = "{\"sealId\":1,\"reason\":\"用印\"}";
        String timestamp = String.valueOf(System.currentTimeMillis());
        String nonce = "nonce-valid-001";

        MockHttpServletRequest request = signedRequest(body, timestamp, nonce, signFor(timestamp, nonce, body));
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainInvoked = new AtomicBoolean(false);
        AtomicReference<String> downstreamBody = new AtomicReference<>();

        filter.doFilter(
                request,
                response,
                (req, res) -> {
                    chainInvoked.set(true);
                    // 下游仍应能重复读取请求体（缓存包装器语义）
                    downstreamBody.set(new String(
                            ((HttpServletRequest) req).getInputStream().readAllBytes(),
                            StandardCharsets.UTF_8));
                });

        assertThat(chainInvoked.get()).as("合法签名应放行到下游过滤器链").isTrue();
        assertThat(downstreamBody.get()).as("下游应读到原始请求体").isEqualTo(body);
        assertThat(response.getStatus()).as("放行时不应写入错误响应").isEqualTo(200);
        verify(nonceStore).tryAcquire(eq(nonce), eq(WINDOW_MILLIS));
    }

    @ParameterizedTest(name = "缺少 {0} 头时被拒绝")
    @ValueSource(strings = {"X-Sign", "X-Timestamp"})
    void missingRequiredHeaderIsRejected(String missingHeader) throws Exception {
        String body = "{}";
        String timestamp = String.valueOf(System.currentTimeMillis());
        String nonce = "nonce-missing-" + missingHeader;

        MockHttpServletRequest request = signedRequest(
                body,
                "X-Timestamp".equals(missingHeader) ? null : timestamp,
                nonce,
                "X-Sign".equals(missingHeader) ? null : signFor(timestamp, nonce, body));
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainInvoked = new AtomicBoolean(false);

        filter.doFilter(request, response, (req, res) -> chainInvoked.set(true));

        assertThat(chainInvoked.get()).as("缺少 %s 时不应放行", missingHeader).isFalse();
        assertErrorResponse(response, ErrorCode.SIGN_INVALID);
    }

    @Test
    void timestampOutsideWindowIsRejected() throws Exception {
        String body = "{}";
        String nonce = "nonce-expired-001";
        // 当前时间往前 10 分钟，超出 ±5 分钟窗口
        String timestamp = String.valueOf(System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(10));

        MockHttpServletRequest request = signedRequest(body, timestamp, nonce, signFor(timestamp, nonce, body));
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainInvoked = new AtomicBoolean(false);

        filter.doFilter(request, response, (req, res) -> chainInvoked.set(true));

        assertThat(chainInvoked.get()).as("时间戳过期不应放行").isFalse();
        assertErrorResponse(response, ErrorCode.SIGN_TIMESTAMP_EXPIRED);
        // 时间窗校验先于 nonce 占用，过期请求不应消耗 nonce
        verify(nonceStore, never()).tryAcquire(anyString(), anyLong());
    }

    @Test
    void replayedNonceIsRejected() throws Exception {
        String body = "{}";
        String timestamp = String.valueOf(System.currentTimeMillis());
        String nonce = "nonce-replay-001";
        // 模拟 nonce 已被占用（重放）
        when(nonceStore.tryAcquire(eq(nonce), anyLong())).thenReturn(false);

        MockHttpServletRequest request = signedRequest(body, timestamp, nonce, signFor(timestamp, nonce, body));
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainInvoked = new AtomicBoolean(false);

        filter.doFilter(request, response, (req, res) -> chainInvoked.set(true));

        assertThat(chainInvoked.get()).as("nonce 重放不应放行").isFalse();
        assertErrorResponse(response, ErrorCode.SIGN_NONCE_REPLAY);
    }

    @Test
    void tamperedBodyIsRejected() throws Exception {
        String originalBody = "{\"amount\":100}";
        String tamperedBody = "{\"amount\":999999}";
        String timestamp = String.valueOf(System.currentTimeMillis());
        String nonce = "nonce-tampered-001";
        // 签名按原始 body 计算，实际请求体被替换
        String sign = signFor(timestamp, nonce, originalBody);

        MockHttpServletRequest request = signedRequest(tamperedBody, timestamp, nonce, sign);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainInvoked = new AtomicBoolean(false);

        filter.doFilter(request, response, (req, res) -> chainInvoked.set(true));

        assertThat(chainInvoked.get()).as("请求体被篡改不应放行").isFalse();
        assertErrorResponse(response, ErrorCode.SIGN_INVALID);
    }

    @Test
    void handlerWithoutHmacSignedAnnotationIsPassedThrough() throws Exception {
        // 命中未标注 @HmacSigned 的普通处理器
        when(handlerMapping.getHandler(any()))
                .thenReturn(new HandlerExecutionChain(
                        new HandlerMethod(controller, controller.plainMethod())));

        // 不带任何签名头，也应直接放行
        MockHttpServletRequest request = new MockHttpServletRequest("POST", REQUEST_PATH);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainInvoked = new AtomicBoolean(false);

        filter.doFilter(request, response, (req, res) -> chainInvoked.set(true));

        assertThat(chainInvoked.get()).as("未标注 @HmacSigned 的处理器应直接放行").isTrue();
        // 未命中签名校验时不应触碰 nonce 存储与密钥解析
        verify(nonceStore, never()).tryAcquire(anyString(), anyLong());
        verify(secretResolver, never()).resolve(any());
    }

    // ------------------------------------------------------------------
    // 辅助方法
    // ------------------------------------------------------------------

    /** 构造带指定请求体与签名头的 Mock 请求；值为 null 时不写入对应头。 */
    private MockHttpServletRequest signedRequest(
            String body, String timestamp, String nonce, String sign) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", REQUEST_PATH);
        request.setContent(body.getBytes(StandardCharsets.UTF_8));
        if (timestamp != null) {
            request.addHeader("X-Timestamp", timestamp);
        }
        if (nonce != null) {
            request.addHeader("X-Nonce", nonce);
        }
        if (sign != null) {
            request.addHeader("X-Sign", sign);
        }
        return request;
    }

    /** 复用生产算法的签名计算，避免测试自行实现一套算法导致与实现脱节。 */
    private String signFor(String timestamp, String nonce, String body) {
        String bodyHash = body.isEmpty() ? "" : HmacSignUtil.sha256Hex(body);
        return HmacSignUtil.sign(timestamp, nonce, bodyHash, DEVICE_SECRET);
    }

    /** 断言过滤器输出的统一错误响应（HTTP 状态码 + 响应体 code/message）。 */
    private void assertErrorResponse(MockHttpServletResponse response, ErrorCode expected)
            throws Exception {
        assertThat(response.getStatus())
                .as("HTTP 状态码应与错误码映射一致")
                .isEqualTo(expected.getHttpStatus().value());
        JsonNode json = objectMapper.readTree(response.getContentAsString(StandardCharsets.UTF_8));
        assertThat(json.path("code").asInt())
                .as("响应体中的业务错误码")
                .isEqualTo(expected.getCode());
        assertThat(json.path("message").asText()).isEqualTo(expected.name());
    }

    /** 测试用控制器：signed() 标注 @HmacSigned，plain() 未标注。 */
    static class SignTestController {

        @HmacSigned
        void signed() {
            // 仅用于提供 HandlerMethod，无需实现
        }

        void plain() {
            // 仅用于提供 HandlerMethod，无需实现
        }

        Method signedMethod() throws NoSuchMethodException {
            return SignTestController.class.getDeclaredMethod("signed");
        }

        Method plainMethod() throws NoSuchMethodException {
            return SignTestController.class.getDeclaredMethod("plain");
        }
    }
}
