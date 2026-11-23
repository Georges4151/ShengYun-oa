package com.ceams.module.ai.client;

import com.ceams.module.ai.support.AiConfigSupport;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * OpenAI 兼容接口客户端（{@code /v1/chat/completions}）。
 *
 * <p>支持 DeepSeek、阿里云百炼、硅基流动、OpenAI 等兼容实现：
 *
 * <ul>
 *   <li>SSE 流式解析：{@code data: {...}} 增量与 {@code data: [DONE]} 结束标记；
 *   <li>{@code delta.content} 文本增量、{@code delta.tool_calls} 按 index 拼接；
 *   <li>鉴权统一 {@code Authorization: Bearer <apiKey>}；
 *   <li>视觉请求使用 content 数组：{@code [{type:text},{type:image_url,image_url:{url:"data:image/jpeg;base64,..."}}}；
 *   <li>拿不到 usage 时留空，不影响主流程。
 * </ul>
 */
@Slf4j
public class OpenAiCompatClient implements AiChatClient {

    /** SSE 数据行前缀。 */
    private static final String DATA_PREFIX = "data:";

    /** SSE 结束标记。 */
    private static final String DONE_MARKER = "[DONE]";

    /** 仅用于静态解析的 ObjectMapper（线程安全）。 */
    private static final ObjectMapper JSON = new ObjectMapper();

    private final String baseUrl;
    private final String apiKey;
    private final String model;
    private final boolean stream;
    private final ObjectMapper objectMapper;
    private final RestClient statusClient;
    private final RestClient chatClient;

    /**
     * 构造器。
     *
     * @param baseUrl 服务地址（通常以 /v1 结尾）
     * @param apiKey API Key，可为空（本地兼容服务可不鉴权）
     * @param model 默认模型名
     * @param stream 是否使用 SSE 流式
     * @param objectMapper JSON 处理器
     * @param statusClient 探活客户端（短超时）
     * @param chatClient 对话客户端（按配置超时）
     */
    public OpenAiCompatClient(
            String baseUrl,
            String apiKey,
            String model,
            boolean stream,
            ObjectMapper objectMapper,
            RestClient statusClient,
            RestClient chatClient) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.model = model;
        this.stream = stream;
        this.objectMapper = objectMapper;
        this.statusClient = statusClient;
        this.chatClient = chatClient;
    }

    @Override
    public boolean isReachable() {
        return probe().success();
    }

    /** 探活：GET /models，区分鉴权失败、地址不可达与其他 HTTP 错误。 */
    @Override
    public AiProbeResult probe() {
        if (!StringUtils.hasText(baseUrl)) {
            return AiProbeResult.fail("未配置服务地址");
        }
        if (!StringUtils.hasText(model)) {
            return AiProbeResult.fail("未配置模型");
        }
        try {
            String body =
                    statusClient
                            .get()
                            .uri("/models")
                            .headers(this::applyAuth)
                            .retrieve()
                            .body(String.class);
            if (!StringUtils.hasText(body)) {
                return AiProbeResult.fail("服务返回空响应");
            }
            JsonNode data = objectMapper.readTree(body).path("data");
            if (data.isArray() && !data.isEmpty() && !containsModel(data)) {
                return AiProbeResult.fail("模型不可用：" + model);
            }
            return AiProbeResult.ok();
        } catch (RestClientResponseException e) {
            int status = e.getStatusCode().value();
            log.warn("OpenAI 兼容探活返回 HTTP {}（{}）", status, AiConfigSupport.sanitizeUrl(baseUrl));
            if (status == 401 || status == 403) {
                return AiProbeResult.fail("鉴权失败(" + status + ")");
            }
            return AiProbeResult.fail("服务返回 HTTP " + status);
        } catch (Exception e) {
            log.warn(
                    "OpenAI 兼容探活失败（{}）：{}",
                    AiConfigSupport.sanitizeUrl(baseUrl),
                    e.getMessage());
            return AiProbeResult.fail("地址无法访问");
        }
    }

    /** 判断 /models 返回列表是否包含当前模型（允许带版本后缀）。 */
    private boolean containsModel(JsonNode data) {
        for (JsonNode item : data) {
            String id = item.path("id").asText("");
            if (id.equals(model) || id.startsWith(model + "-") || model.startsWith(id + "-")) {
                return true;
            }
        }
        return false;
    }

    @Override
    public OllamaChatResult chatStream(
            List<ChatMessage> messages, List<Map<String, Object>> tools, Consumer<String> onDelta) {
        if (messages == null || messages.isEmpty()) {
            throw new IllegalArgumentException("messages 不能为空");
        }
        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", toOpenAiMessages(messages));
        if (tools != null && !tools.isEmpty()) {
            requestBody.put("tools", tools);
        }
        requestBody.put("stream", stream);

        long startNanos = System.nanoTime();
        OllamaChatResult result;
        try {
            result =
                    chatClient
                            .post()
                            .uri("/chat/completions")
                            .headers(this::applyAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.TEXT_EVENT_STREAM)
                            .body(requestBody)
                            .exchange((request, response) -> readStream(response, onDelta));
        } catch (RestClientResponseException e) {
            throw new IllegalStateException(
                    "OpenAI 兼容对话接口返回 HTTP " + e.getStatusCode().value(), e);
        }
        if (result == null) {
            result = new OllamaChatResult();
        }
        result.setLatencyMs((System.nanoTime() - startNanos) / 1_000_000L);
        return result;
    }

    /**
     * 带图对话：content 使用 text + image_url 数组，图片以 data URL 传递。
     */
    @Override
    public String chatWithImages(String requestedModel, String prompt, List<String> images) {
        String actualModel = StringUtils.hasText(requestedModel) ? requestedModel : model;
        if (!StringUtils.hasText(actualModel)) {
            throw new IllegalArgumentException("model 不能为空");
        }
        if (!StringUtils.hasText(prompt)) {
            throw new IllegalArgumentException("prompt 不能为空");
        }
        if (images == null || images.isEmpty()) {
            throw new IllegalArgumentException("images 不能为空");
        }

        List<Map<String, Object>> content = new ArrayList<>(images.size() + 1);
        Map<String, Object> textPart = new LinkedHashMap<>(2);
        textPart.put("type", "text");
        textPart.put("text", prompt);
        content.add(textPart);
        for (String image : images) {
            Map<String, Object> imageUrl = new LinkedHashMap<>(1);
            imageUrl.put("url", toDataUrl(image));
            Map<String, Object> imagePart = new LinkedHashMap<>(2);
            imagePart.put("type", "image_url");
            imagePart.put("image_url", imageUrl);
            content.add(imagePart);
        }
        Map<String, Object> userMessage = new LinkedHashMap<>(2);
        userMessage.put("role", "user");
        userMessage.put("content", content);

        Map<String, Object> requestBody = new LinkedHashMap<>(4);
        requestBody.put("model", actualModel);
        requestBody.put("stream", false);
        requestBody.put("messages", List.of(userMessage));

        String body;
        try {
            body =
                    chatClient
                            .post()
                            .uri("/chat/completions")
                            .headers(this::applyAuth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .body(requestBody)
                            .retrieve()
                            .body(String.class);
        } catch (RestClientResponseException e) {
            throw new IllegalStateException(
                    "OpenAI 兼容图像对话返回 HTTP " + e.getStatusCode().value(), e);
        }
        if (!StringUtils.hasText(body)) {
            throw new IllegalStateException("OpenAI 兼容图像对话返回空响应");
        }
        try {
            String text =
                    objectMapper
                            .readTree(body)
                            .path("choices")
                            .path(0)
                            .path("message")
                            .path("content")
                            .asText("");
            if (!StringUtils.hasText(text)) {
                throw new IllegalStateException("OpenAI 兼容图像对话响应缺少 content");
            }
            return text;
        } catch (IOException e) {
            throw new IllegalStateException("OpenAI 兼容图像对话响应解析失败", e);
        }
    }

    /** 图片 base64 转 data URL；已是 data URL 时原样返回。 */
    private static String toDataUrl(String image) {
        String value = image == null ? "" : image.trim();
        if (value.startsWith("data:")) {
            return value;
        }
        return "data:image/jpeg;base64," + value;
    }

    /** 统一追加 Bearer 鉴权（Key 为空时不发送 Authorization 头）。 */
    private void applyAuth(HttpHeaders headers) {
        if (StringUtils.hasText(apiKey)) {
            headers.setBearerAuth(apiKey.trim());
        }
    }

    /**
     * 转换为 OpenAI messages 结构。
     *
     * <p>助手消息的 tool_calls 需要 JSON 字符串形式的 arguments，并生成 {@code call_x} 形式的 id；
     * 紧随其后的 tool 消息按顺序绑定 tool_call_id，保证工具结果能正确回灌。
     */
    private List<Map<String, Object>> toOpenAiMessages(List<ChatMessage> messages) {
        List<Map<String, Object>> payload = new ArrayList<>(messages.size());
        Deque<String> pendingToolIds = new ArrayDeque<>();
        int sequence = 0;
        for (ChatMessage message : messages) {
            Map<String, Object> item = new LinkedHashMap<>(3);
            item.put("role", message.role());
            item.put("content", message.content());
            if ("assistant".equals(message.role()) && !message.toolCalls().isEmpty()) {
                List<Map<String, Object>> calls = new ArrayList<>(message.toolCalls().size());
                for (OllamaToolCall call : message.toolCalls()) {
                    String id = "call_" + (sequence++);
                    pendingToolIds.addLast(id);
                    Map<String, Object> function = new LinkedHashMap<>(2);
                    function.put("name", call.name());
                    function.put("arguments", writeJson(call.arguments()));
                    Map<String, Object> callItem = new LinkedHashMap<>(3);
                    callItem.put("id", id);
                    callItem.put("type", "function");
                    callItem.put("function", function);
                    calls.add(callItem);
                }
                item.put("tool_calls", calls);
            }
            if ("tool".equals(message.role())) {
                String toolCallId = pendingToolIds.pollFirst();
                if (toolCallId != null) {
                    item.put("tool_call_id", toolCallId);
                }
            }
            payload.add(item);
        }
        return payload;
    }

    private String writeJson(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (Exception e) {
            return "{}";
        }
    }

    /** 读取 SSE 响应体并汇总增量文本、工具调用与用量。 */
    private OllamaChatResult readStream(ClientHttpResponse response, Consumer<String> onDelta)
            throws IOException {
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException(
                    "OpenAI 兼容对话接口返回 HTTP " + response.getStatusCode().value());
        }
        OllamaChatResult result = new OllamaChatResult();
        StringBuilder content = new StringBuilder();
        Map<Integer, ToolCallAccumulator> toolCalls = new TreeMap<>();

        InputStream body = response.getBody();
        if (body == null) {
            return result;
        }
        try (BufferedReader reader =
                new BufferedReader(new InputStreamReader(body, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String payload = extractData(line);
                if (payload == null) {
                    continue;
                }
                if (DONE_MARKER.equals(payload)) {
                    break;
                }
                JsonNode root;
                try {
                    root = objectMapper.readTree(payload);
                } catch (Exception e) {
                    continue;
                }
                JsonNode delta = root.path("choices").path(0).path("delta");
                JsonNode contentNode = delta.path("content");
                if (contentNode.isTextual()) {
                    String deltaText = contentNode.asText("");
                    if (!deltaText.isEmpty()) {
                        content.append(deltaText);
                        if (onDelta != null) {
                            onDelta.accept(deltaText);
                        }
                    }
                }
                JsonNode calls = delta.path("tool_calls");
                if (calls.isArray()) {
                    for (JsonNode call : calls) {
                        int index = call.path("index").asInt(0);
                        ToolCallAccumulator accumulator =
                                toolCalls.computeIfAbsent(index, k -> new ToolCallAccumulator());
                        String id = call.path("id").asText("");
                        if (StringUtils.hasText(id)) {
                            accumulator.id = id;
                        }
                        JsonNode function = call.path("function");
                        String name = function.path("name").asText("");
                        if (StringUtils.hasText(name)) {
                            accumulator.name.append(name);
                        }
                        accumulator.arguments.append(function.path("arguments").asText(""));
                    }
                }
                JsonNode usage = root.path("usage");
                if (usage.isObject() && usage.has("prompt_tokens")) {
                    result.setPromptTokens(usage.path("prompt_tokens").asInt(0));
                    result.setCompletionTokens(usage.path("completion_tokens").asInt(0));
                }
            }
        }
        result.setContent(content.toString());
        result.setToolCalls(toToolCalls(toolCalls));
        return result;
    }

    private List<OllamaToolCall> toToolCalls(Map<Integer, ToolCallAccumulator> accumulators) {
        List<OllamaToolCall> calls = new ArrayList<>(accumulators.size());
        for (ToolCallAccumulator accumulator : accumulators.values()) {
            String name = accumulator.name.toString().trim();
            if (name.isEmpty()) {
                continue;
            }
            calls.add(new OllamaToolCall(name, parseArguments(accumulator.arguments.toString())));
        }
        return calls;
    }

    private static Map<String, Object> parseArguments(String rawArguments) {
        if (!StringUtils.hasText(rawArguments)) {
            return Map.of();
        }
        try {
            JsonNode node = JSON.readTree(rawArguments);
            if (!node.isObject()) {
                return Map.of();
            }
            return JSON.convertValue(node, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }

    /**
     * 解析单行 SSE 的文本增量。
     *
     * @param line 原始行（可带 {@code data:} 前缀）
     * @return 非空增量文本；注释行、空 content、非法 JSON 返回空
     */
    public static Optional<String> parseSseContent(String line) {
        String payload = extractData(line);
        if (payload == null || DONE_MARKER.equals(payload)) {
            return Optional.empty();
        }
        try {
            JsonNode content =
                    JSON.readTree(payload)
                            .path("choices")
                            .path(0)
                            .path("delta")
                            .path("content");
            if (!content.isTextual()) {
                return Optional.empty();
            }
            String text = content.asText("");
            return text.isEmpty() ? Optional.empty() : Optional.of(text);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * 解析单行 SSE 的工具调用增量。
     *
     * @param line 原始行
     * @return 本行的工具调用片段；无 tool_calls 或非法行返回空列表
     */
    public static List<ToolCallDelta> parseSseToolCalls(String line) {
        String payload = extractData(line);
        if (payload == null || DONE_MARKER.equals(payload)) {
            return List.of();
        }
        try {
            JsonNode calls =
                    JSON.readTree(payload).path("choices").path(0).path("delta").path("tool_calls");
            if (!calls.isArray() || calls.isEmpty()) {
                return List.of();
            }
            List<ToolCallDelta> deltas = new ArrayList<>(calls.size());
            for (JsonNode call : calls) {
                JsonNode function = call.path("function");
                deltas.add(
                        new ToolCallDelta(
                                call.path("index").asInt(0),
                                call.path("id").asText(""),
                                function.path("name").asText(""),
                                function.path("arguments").asText("")));
            }
            return deltas;
        } catch (Exception e) {
            return List.of();
        }
    }

    /**
     * 解析单行 SSE 的 token 用量（部分实现仅在最后一个 chunk 返回）。
     *
     * @param line 原始行
     * @return 同时含 prompt_tokens/completion_tokens 时返回用量；否则空
     */
    public static Optional<OllamaUsage> parseSseUsage(String line) {
        String payload = extractData(line);
        if (payload == null || DONE_MARKER.equals(payload)) {
            return Optional.empty();
        }
        try {
            JsonNode usage = JSON.readTree(payload).path("usage");
            if (!usage.has("prompt_tokens") || !usage.has("completion_tokens")) {
                return Optional.empty();
            }
            return Optional.of(
                    new OllamaUsage(
                            usage.path("prompt_tokens").asInt(0),
                            usage.path("completion_tokens").asInt(0)));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /** 提取 {@code data:} 后的负载；非数据行返回 null。 */
    private static String extractData(String line) {
        if (line == null || line.isBlank()) {
            return null;
        }
        String trimmed = line.trim();
        if (!trimmed.startsWith(DATA_PREFIX)) {
            return null;
        }
        return trimmed.substring(DATA_PREFIX.length()).trim();
    }

    /** SSE 工具调用片段。 */
    public record ToolCallDelta(int index, String id, String name, String arguments) {}

    /** 按 index 累加工具调用。 */
    private static final class ToolCallAccumulator {
        private String id;
        private final StringBuilder name = new StringBuilder();
        private final StringBuilder arguments = new StringBuilder();
    }
}
