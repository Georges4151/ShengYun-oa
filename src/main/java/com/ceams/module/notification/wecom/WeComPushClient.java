package com.ceams.module.notification.wecom;

import com.ceams.module.notification.config.WeComConfiguration;
import com.ceams.module.notification.config.WeComProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

/**
 * 企业微信群机器人推送客户端。
 *
 * <p>最小可用设计：仅使用群机器人 webhook + markdown 消息，不依赖企业微信应用凭证，也不按用户路由。
 *
 * <ul>
 *   <li>未启用、webhook 为空或非 http(s) 时直接跳过，不发起外部请求；
 *   <li>发送在独立线程池中执行，线程池拒绝时仅记录 warn；
 *   <li>任何异常（网络、超时、非 0 errcode）都只记录日志，绝不向审批主流程抛出。
 * </ul>
 */
@Slf4j
@Component
public class WeComPushClient {

    /** 企业微信响应中的错误码字段。 */
    private static final String KEY_ERRCODE = "errcode";

    /** 企业微信响应中的错误信息字段。 */
    private static final String KEY_ERRMSG = "errmsg";

    private final WeComProperties properties;
    private final TaskExecutor executor;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    /** 生产构造器：按名称注入推送线程池，避免与 AI 对话线程池冲突。 */
    @Autowired
    public WeComPushClient(
            WeComProperties properties,
            @Qualifier(WeComConfiguration.EXECUTOR_BEAN_NAME) TaskExecutor executor,
            ObjectMapper objectMapper,
            RestClient.Builder restClientBuilder) {
        this(properties, executor, objectMapper, buildClient(restClientBuilder, properties));
    }

    /** 包级构造器：便于单测注入已绑定 MockRestServiceServer 的 RestClient。 */
    WeComPushClient(
            WeComProperties properties,
            TaskExecutor executor,
            ObjectMapper objectMapper,
            RestClient restClient) {
        this.properties = properties;
        this.executor = executor;
        this.objectMapper = objectMapper;
        this.restClient = restClient;
    }

    /**
     * 异步推送一条企业微信消息。
     *
     * @param title 消息标题
     * @param content 消息正文（可含换行）
     * @param linkUrl 详情链接；仅 http/https 会以「查看详情」形式追加
     */
    public void pushAsync(String title, String content, String linkUrl) {
        if (!properties.isEnabled()) {
            log.debug("企业微信推送未启用，跳过：title={}", title);
            return;
        }
        if (!isHttpUrl(properties.getWebhookUrl())) {
            log.debug("企业微信 webhook 未配置或非法，跳过：title={}", title);
            return;
        }
        try {
            executor.execute(() -> push(title, content, linkUrl));
        } catch (TaskRejectedException e) {
            log.warn("企业微信推送任务被拒绝（队列已满，已忽略）：title={}", title);
        }
    }

    /**
     * 同步推送（包级可见，便于单测直接断言）。
     *
     * @return 是否推送成功；任何异常均返回 false
     */
    boolean push(String title, String content, String linkUrl) {
        String webhookUrl = properties.getWebhookUrl() == null ? "" : properties.getWebhookUrl().trim();
        if (!isHttpUrl(webhookUrl)) {
            return false;
        }
        try {
            String body =
                    restClient
                            .post()
                            .uri(webhookUrl)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(buildPayload(title, content, linkUrl))
                            .retrieve()
                            .body(String.class);
            if (!StringUtils.hasText(body)) {
                log.warn("企业微信推送返回空响应：title={}", title);
                return false;
            }
            JsonNode node = objectMapper.readTree(body);
            int errcode = node.path(KEY_ERRCODE).asInt(-1);
            if (errcode != 0) {
                log.warn(
                        "企业微信推送失败：title={}，errcode={}，errmsg={}",
                        title,
                        errcode,
                        node.path(KEY_ERRMSG).asText(""));
                return false;
            }
            log.info("企业微信推送成功：title={}", title);
            return true;
        } catch (Exception e) {
            log.warn("企业微信推送异常（已忽略，不影响业务流程）：title={}，原因={}", title, e.getMessage());
            return false;
        }
    }

    /** 组装 markdown 消息体。 */
    private Map<String, Object> buildPayload(String title, String content, String linkUrl) {
        StringBuilder markdown = new StringBuilder();
        markdown.append("### ").append(safeTitle(title)).append('\n');
        String safeContent = truncate(content, Math.max(0, properties.getMaxContentLength()));
        if (StringUtils.hasText(safeContent)) {
            for (String line : safeContent.split("\n", -1)) {
                markdown.append("> ").append(line).append('\n');
            }
        }
        if (isHttpUrl(linkUrl)) {
            markdown.append("\n[查看详情](").append(linkUrl.trim()).append(')');
        }
        Map<String, Object> markdownBody = new LinkedHashMap<>();
        markdownBody.put("content", markdown.toString());
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("msgtype", "markdown");
        payload.put("markdown", markdownBody);
        return payload;
    }

    private String safeTitle(String title) {
        return StringUtils.hasText(title) ? title.trim() : "CEAMS 通知";
    }

    private String truncate(String content, int maxLength) {
        if (!StringUtils.hasText(content)) {
            return "";
        }
        String trimmed = content.trim();
        if (maxLength <= 0 || trimmed.length() <= maxLength) {
            return trimmed;
        }
        return trimmed.substring(0, maxLength) + "…";
    }

    private boolean isHttpUrl(String url) {
        if (!StringUtils.hasText(url)) {
            return false;
        }
        String value = url.trim().toLowerCase();
        return value.startsWith("http://") || value.startsWith("https://");
    }

    /** 构建带超时的 RestClient（连接与读取超时一致）。 */
    private static RestClient buildClient(RestClient.Builder builder, WeComProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        int timeout = Math.max(500, properties.getTimeoutMillis());
        factory.setConnectTimeout(timeout);
        factory.setReadTimeout(timeout);
        return builder.requestFactory(factory).build();
    }
}
