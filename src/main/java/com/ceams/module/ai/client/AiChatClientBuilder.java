package com.ceams.module.ai.client;

import com.ceams.module.ai.config.AiPurposeConfig;
import com.ceams.module.ai.enums.AiProvider;
import com.ceams.module.ai.support.AiConfigSupport;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * 按运行时配置构建具体 {@link AiChatClient}。
 *
 * <p>与配置服务解耦（不反向依赖 {@code AiSettingsService}），既供路由工厂缓存使用，
 * 也供管理员连接测试构建一次性客户端，避免循环依赖。
 */
@Component
@RequiredArgsConstructor
public class AiChatClientBuilder {

    /** 探活连接超时：2 秒。 */
    private static final int PROBE_CONNECT_TIMEOUT_MILLIS = 2_000;

    /** 探活读取超时：5 秒（互联网厂商 RTT 可能较高）。 */
    private static final int PROBE_READ_TIMEOUT_MILLIS = 5_000;

    /** 对话连接超时：2 秒。 */
    private static final int CHAT_CONNECT_TIMEOUT_MILLIS = 2_000;

    private final RestClient.Builder restClientBuilder;
    private final ObjectMapper objectMapper;

    /**
     * 构建客户端。
     *
     * @param config 运行时配置
     * @return Ollama 或 OpenAI 兼容实现
     */
    public AiChatClient create(AiPurposeConfig config) {
        String baseUrl = AiConfigSupport.normalizeBaseUrl(config.getBaseUrl());
        String model = config.getModel() == null ? "" : config.getModel().trim();
        int timeoutSeconds = Math.max(1, config.getTimeoutSeconds());
        RestClient statusClient =
                buildClient(baseUrl, PROBE_CONNECT_TIMEOUT_MILLIS, PROBE_READ_TIMEOUT_MILLIS);
        RestClient chatClient =
                buildClient(baseUrl, CHAT_CONNECT_TIMEOUT_MILLIS, timeoutSeconds * 1000);
        if (config.getProvider() == AiProvider.OPENAI_COMPAT) {
            return new OpenAiCompatClient(
                    baseUrl,
                    config.getApiKey(),
                    model,
                    true,
                    objectMapper,
                    statusClient,
                    chatClient);
        }
        return new OllamaClient(
                baseUrl, model, true, objectMapper, statusClient, chatClient);
    }

    /** 构建带独立超时的 RestClient。 */
    private RestClient buildClient(String baseUrl, int connectTimeoutMillis, int readTimeoutMillis) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeoutMillis);
        requestFactory.setReadTimeout(readTimeoutMillis);
        return restClientBuilder.clone().baseUrl(baseUrl).requestFactory(requestFactory).build();
    }
}
