package com.ceams.module.ai.client;

import com.ceams.module.ai.config.AiProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

/**
 * 本地 Ollama 轻量客户端（零新增依赖，基于 Spring 6.1 {@link RestClient}）。
 *
 * <ul>
 *   <li>{@link #isReachable()}：GET {@code /api/tags} 探活，异常一律吞掉并返回 false，不向上抛。
 *   <li>{@link #chatStream(List, Consumer)}：POST {@code /api/chat}，按 NDJSON 逐行回调增量文本， 并汇总 token
 *       用量与耗时。
 * </ul>
 *
 * <p>探活固定使用短超时（连接 2 秒 / 读取 3 秒），避免 {@code /api/v1/ai/status} 被慢速 Ollama 拖死； 对话读取超时使用 {@code
 * ceams.ai.timeout-seconds}。
 */
@Slf4j
@Component
public class OllamaClient {

    /** 探活连接超时：2 秒。 */
    private static final int PROBE_CONNECT_TIMEOUT_MILLIS = 2_000;

    /** 探活读取超时：3 秒。 */
    private static final int PROBE_READ_TIMEOUT_MILLIS = 3_000;

    /** 对话连接超时：2 秒（连接建立与生成长度无关）。 */
    private static final int CHAT_CONNECT_TIMEOUT_MILLIS = 2_000;

    /** 仅用于 NDJSON 静态解析的 ObjectMapper（线程安全，配置完成后可共享）。 */
    private static final ObjectMapper JSON = new ObjectMapper();

    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper;
    private final RestClient statusClient;
    private final RestClient chatClient;

    /** 生产用构造器：显式标注 {@code @Autowired}，避免与测试用包级构造器二选一失败。 */
    @Autowired
    public OllamaClient(
            RestClient.Builder restClientBuilder,
            AiProperties aiProperties,
            ObjectMapper objectMapper) {
        this(
                aiProperties,
                objectMapper,
                buildClient(
                        restClientBuilder,
                        aiProperties,
                        PROBE_CONNECT_TIMEOUT_MILLIS,
                        PROBE_READ_TIMEOUT_MILLIS),
                buildClient(
                        restClientBuilder,
                        aiProperties,
                        CHAT_CONNECT_TIMEOUT_MILLIS,
                        Math.max(1, aiProperties.getTimeoutSeconds()) * 1000));
    }

    /** 供单元测试注入已绑定 {@code MockRestServiceServer} 的 RestClient。 */
    OllamaClient(
            AiProperties aiProperties,
            ObjectMapper objectMapper,
            RestClient statusClient,
            RestClient chatClient) {
        this.aiProperties = aiProperties;
        this.objectMapper = objectMapper;
        this.statusClient = statusClient;
        this.chatClient = chatClient;
    }

    /**
     * 探活：HTTP 2xx 且模型列表中包含配置的模型才视为可用。
     *
     * <p>连接拒绝、超时、非 2xx、响应解析失败、模型未下载等任何异常都返回 false， 且不向调用方抛出。
     */
    public boolean isReachable() {
        String model = aiProperties.getModel();
        if (!StringUtils.hasText(model)) {
            log.warn("未配置 ceams.ai.model，Ollama 视为不可用");
            return false;
        }
        try {
            String body = statusClient.get().uri("/api/tags").retrieve().body(String.class);
            if (body == null || body.isBlank()) {
                log.warn("Ollama /api/tags 返回空响应，视为不可用");
                return false;
            }
            JsonNode models = objectMapper.readTree(body).path("models");
            if (!models.isArray()) {
                log.warn("Ollama /api/tags 响应缺少 models 数组，视为不可用");
                return false;
            }
            for (JsonNode modelNode : models) {
                if (matchesModel(modelNode.path("name").asText(""), model)) {
                    return true;
                }
            }
            log.warn("Ollama 可达但未找到模型 {}，请先执行 ollama pull {}", model, model);
            return false;
        } catch (Exception e) {
            log.warn("Ollama 探活失败（{}）：{}", aiProperties.getBaseUrl(), e.getMessage());
            return false;
        }
    }

    /**
     * 流式对话（无工具，兼容 M1 两参调用）。
     *
     * @param messages 完整上下文消息（含 system 提示）
     * @param onDelta 增量文本回调，允许为 null（只关心最终结果时）
     * @return 汇总后的回答文本、工具调用、token 用量与耗时
     */
    public OllamaChatResult chatStream(List<ChatMessage> messages, Consumer<String> onDelta) {
        return chatStream(messages, List.of(), onDelta);
    }

    /**
     * 流式对话：逐行解析 NDJSON，每拿到一段 {@code message.content} 增量就回调 {@code onDelta}， 同时累积 {@code
     * message.tool_calls}。
     *
     * @param messages 完整上下文消息（含 system 提示）
     * @param tools Ollama {@code tools} 数组；为空时不向请求体写入该字段
     * @param onDelta 增量文本回调，允许为 null（只关心最终结果时）
     * @return 汇总后的回答文本、工具调用、token 用量与耗时
     */
    public OllamaChatResult chatStream(
            List<ChatMessage> messages, List<Map<String, Object>> tools, Consumer<String> onDelta) {
        if (messages == null || messages.isEmpty()) {
            throw new IllegalArgumentException("messages 不能为空");
        }

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", aiProperties.getModel());
        List<Map<String, Object>> payloadMessages = new ArrayList<>(messages.size());
        for (ChatMessage message : messages) {
            Map<String, Object> item = new LinkedHashMap<>(3);
            item.put("role", message.role());
            item.put("content", message.content());
            if (!message.toolCalls().isEmpty()) {
                item.put("tool_calls", toPayloadToolCalls(message.toolCalls()));
            }
            payloadMessages.add(item);
        }
        requestBody.put("messages", payloadMessages);
        if (tools != null && !tools.isEmpty()) {
            requestBody.put("tools", tools);
        }
        requestBody.put("stream", aiProperties.isStream());

        long startNanos = System.nanoTime();
        OllamaChatResult result =
                chatClient
                        .post()
                        .uri("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_NDJSON)
                        .body(requestBody)
                        .exchange((request, response) -> readStream(response, onDelta));
        if (result == null) {
            result = new OllamaChatResult();
        }
        result.setLatencyMs((System.nanoTime() - startNanos) / 1_000_000L);
        return result;
    }

    /**
     * 解析单行 NDJSON 的增量文本。
     *
     * @return 非空增量文本；空 content、非法 JSON、缺字段均返回 {@link Optional#empty()}
     */
    public static Optional<String> parseDelta(String line) {
        if (line == null || line.isBlank()) {
            return Optional.empty();
        }
        try {
            JsonNode contentNode = JSON.readTree(line).path("message").path("content");
            if (contentNode.isMissingNode() || contentNode.isNull()) {
                return Optional.empty();
            }
            String content = contentNode.asText("");
            return content.isEmpty() ? Optional.empty() : Optional.of(content);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * 解析单行 NDJSON 的 {@code message.tool_calls}。
     *
     * <p>Ollama 的 {@code arguments} 可能是 JSON 对象，也可能是 JSON 字符串（不同版本/模型模板行为不一致）， 两种形态都统一解析为 Map。
     *
     * @return 本行请求的工具调用；无字段、非法 JSON、缺工具名均返回空列表
     */
    public static List<OllamaToolCall> parseToolCalls(String line) {
        if (line == null || line.isBlank()) {
            return List.of();
        }
        try {
            JsonNode calls = JSON.readTree(line).path("message").path("tool_calls");
            if (!calls.isArray() || calls.isEmpty()) {
                return List.of();
            }
            List<OllamaToolCall> parsed = new ArrayList<>(calls.size());
            for (JsonNode call : calls) {
                JsonNode function = call.path("function");
                String name = function.path("name").asText("").trim();
                if (name.isEmpty()) {
                    continue;
                }
                parsed.add(new OllamaToolCall(name, parseArguments(function.path("arguments"))));
            }
            return parsed;
        } catch (Exception e) {
            return List.of();
        }
    }

    /**
     * 解析单行 NDJSON 的 token 用量（Ollama 仅在 {@code done:true} 的末行返回）。
     *
     * @return 同时存在 {@code prompt_eval_count} 与 {@code eval_count} 时返回用量；非法行/缺字段返回空
     */
    public static Optional<OllamaUsage> parseUsage(String line) {
        if (line == null || line.isBlank()) {
            return Optional.empty();
        }
        try {
            JsonNode root = JSON.readTree(line);
            JsonNode promptNode = root.path("prompt_eval_count");
            JsonNode evalNode = root.path("eval_count");
            if (promptNode.isMissingNode() || evalNode.isMissingNode()) {
                return Optional.empty();
            }
            return Optional.of(new OllamaUsage(promptNode.asInt(0), evalNode.asInt(0)));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /** 解析单行 NDJSON 是否表示流结束（{@code done:true}）。 */
    public static boolean isDone(String line) {
        if (line == null || line.isBlank()) {
            return false;
        }
        try {
            JsonNode doneNode = JSON.readTree(line).path("done");
            return doneNode.isBoolean() && doneNode.asBoolean();
        } catch (Exception e) {
            return false;
        }
    }

    /** 按行读取 NDJSON 响应体，返回汇总结果。try-with-resources 保证响应流必定关闭。 */
    private OllamaChatResult readStream(ClientHttpResponse response, Consumer<String> onDelta)
            throws IOException {
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException(
                    "Ollama 对话接口返回 HTTP " + response.getStatusCode().value());
        }

        OllamaChatResult result = new OllamaChatResult();
        StringBuilder content = new StringBuilder();
        List<OllamaToolCall> toolCalls = new ArrayList<>();
        InputStream body = response.getBody();
        if (body == null) {
            return result;
        }
        try (BufferedReader reader =
                new BufferedReader(new InputStreamReader(body, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                parseDelta(line)
                        .ifPresent(
                                delta -> {
                                    content.append(delta);
                                    if (onDelta != null) {
                                        onDelta.accept(delta);
                                    }
                                });
                toolCalls.addAll(parseToolCalls(line));
                if (isDone(line)) {
                    parseUsage(line)
                            .ifPresent(
                                    usage -> {
                                        result.setPromptTokens(usage.promptTokens());
                                        result.setCompletionTokens(usage.completionTokens());
                                    });
                    break;
                }
            }
        }
        result.setContent(content.toString());
        result.setToolCalls(toolCalls);
        return result;
    }

    /** 将工具调用转换为 Ollama 请求体格式（{@code [{"function":{"name":..,"arguments":..}}]}）。 */
    private static List<Map<String, Object>> toPayloadToolCalls(List<OllamaToolCall> toolCalls) {
        List<Map<String, Object>> payload = new ArrayList<>(toolCalls.size());
        for (OllamaToolCall call : toolCalls) {
            Map<String, Object> function = new LinkedHashMap<>(2);
            function.put("name", call.name());
            function.put("arguments", call.arguments());
            Map<String, Object> item = new LinkedHashMap<>(1);
            item.put("function", function);
            payload.add(item);
        }
        return payload;
    }

    /** 兼容 arguments 的两种形态：JSON 对象 / JSON 字符串；无法解析时返回空参数。 */
    private static Map<String, Object> parseArguments(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return Map.of();
        }
        try {
            if (node.isTextual()) {
                String text = node.asText("").trim();
                if (text.isEmpty()) {
                    return Map.of();
                }
                JsonNode parsed = JSON.readTree(text);
                if (!parsed.isObject()) {
                    return Map.of();
                }
                return JSON.convertValue(parsed, new TypeReference<Map<String, Object>>() {});
            }
            if (node.isObject()) {
                return JSON.convertValue(node, new TypeReference<Map<String, Object>>() {});
            }
        } catch (Exception e) {
            return Map.of();
        }
        return Map.of();
    }

    /** 模型名匹配：精确相等；配置未带 tag 时允许匹配同名模型（如 qwen2.5 → qwen2.5:7b）。 */
    private static boolean matchesModel(String name, String expected) {
        if (name == null || name.isEmpty() || expected == null || expected.isEmpty()) {
            return false;
        }
        if (name.equals(expected)) {
            return true;
        }
        return !expected.contains(":") && name.startsWith(expected + ":");
    }

    /** 从注入的 {@link RestClient.Builder} 克隆出带超时与 baseUrl 的独立客户端。 */
    private static RestClient buildClient(
            RestClient.Builder source,
            AiProperties aiProperties,
            int connectTimeoutMillis,
            int readTimeoutMillis) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeoutMillis);
        factory.setReadTimeout(readTimeoutMillis);
        return source.clone()
                .baseUrl(normalizeBaseUrl(aiProperties.getBaseUrl()))
                .requestFactory(factory)
                .build();
    }

    private static String normalizeBaseUrl(String baseUrl) {
        if (!StringUtils.hasText(baseUrl)) {
            return "http://localhost:11434";
        }
        String trimmed = baseUrl.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }
}
