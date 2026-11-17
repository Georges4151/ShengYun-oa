package com.ceams.module.notification.wecom;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.ceams.module.notification.config.WeComProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** {@link WeComPushClient} 单元测试：开关、消息体、截断与异常降级。 */
class WeComPushClientTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String WEBHOOK_URL =
            "https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=test";

    /** 同步执行的线程池，保证断言时请求已发出。 */
    private static final TaskExecutor DIRECT_EXECUTOR = Runnable::run;

    private final WeComProperties properties = new WeComProperties();

    private final RestClient.Builder builder = RestClient.builder();

    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

    private WeComPushClient client() {
        return new WeComPushClient(properties, DIRECT_EXECUTOR, MAPPER, builder.build());
    }

    @Test
    void 未启用时不发起请求() {
        properties.setEnabled(false);
        properties.setWebhookUrl(WEBHOOK_URL);

        assertDoesNotThrow(() -> client().pushAsync("标题", "正文", null));
        server.verify();
    }

    @Test
    void webhook为空或非http时跳过() {
        properties.setEnabled(true);
        properties.setWebhookUrl("");
        assertDoesNotThrow(() -> client().pushAsync("标题", "正文", null));

        properties.setWebhookUrl("ftp://example.com/hook");
        assertDoesNotThrow(() -> client().pushAsync("标题", "正文", null));
        server.verify();
    }

    @Test
    void 启用后发送markdown并追加详情链接() {
        properties.setEnabled(true);
        properties.setWebhookUrl(WEBHOOK_URL);
        server.expect(requestTo(WEBHOOK_URL))
                .andExpect(jsonPath("$.msgtype").value("markdown"))
                .andExpect(jsonPath("$.markdown.content", containsString("### 新的报销待办")))
                .andExpect(jsonPath("$.markdown.content", containsString("请及时处理")))
                .andExpect(
                        jsonPath(
                                "$.markdown.content",
                                containsString("[查看详情](https://ceams.example.com/task/1)")))
                .andRespond(withSuccess("{\"errcode\":0,\"errmsg\":\"ok\"}", MediaType.APPLICATION_JSON));

        assertTrue(client().push("新的报销待办", "请及时处理", "https://ceams.example.com/task/1"));
        server.verify();
    }

    @Test
    void 正文超长时截断() {
        properties.setEnabled(true);
        properties.setWebhookUrl(WEBHOOK_URL);
        properties.setMaxContentLength(5);
        server.expect(requestTo(WEBHOOK_URL))
                .andExpect(jsonPath("$.markdown.content", containsString("12345…")))
                .andExpect(jsonPath("$.markdown.content", not(containsString("123456"))))
                .andRespond(withSuccess("{\"errcode\":0,\"errmsg\":\"ok\"}", MediaType.APPLICATION_JSON));

        assertTrue(client().push("标题", "1234567890", null));
        server.verify();
    }

    @Test
    void 错误码非零时返回false且不抛异常() {
        properties.setEnabled(true);
        properties.setWebhookUrl(WEBHOOK_URL);
        server.expect(requestTo(WEBHOOK_URL))
                .andRespond(
                        withSuccess(
                                "{\"errcode\":93000,\"errmsg\":\"invalid webhook url\"}",
                                MediaType.APPLICATION_JSON));

        assertFalse(client().push("标题", "正文", null));
        server.verify();
    }

    @Test
    void 网络异常时返回false且不抛异常() {
        properties.setEnabled(true);
        properties.setWebhookUrl(WEBHOOK_URL);
        server.expect(requestTo(WEBHOOK_URL))
                .andRespond(
                        request -> {
                            throw new IOException("connection refused");
                        });

        assertFalse(client().push("标题", "正文", null));
        server.verify();
    }
}
