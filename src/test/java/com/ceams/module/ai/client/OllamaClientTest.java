package com.ceams.module.ai.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.ceams.module.ai.config.AiProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * {@link OllamaClient} 单元测试：探活（模型存在 / 不存在 / 非 2xx / 连接异常）、NDJSON 增量与用量解析、 流式汇总。
 *
 * <p>全程使用 {@link MockRestServiceServer} 与纯静态解析方法，不依赖真实 Ollama。
 */
class OllamaClientTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String BASE_URL = "http://localhost:11434";
    private static final String TAGS_URL = BASE_URL + "/api/tags";
    private static final String CHAT_URL = BASE_URL + "/api/chat";

    private final AiProperties properties = new AiProperties();
    private final RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    private final OllamaClient client =
            new OllamaClient(properties, MAPPER, builder.build(), builder.build());

    @Test
    void 探活_模型存在_返回true() {
        server.expect(requestTo(TAGS_URL))
                .andRespond(
                        withSuccess(
                                "{\"models\":[{\"name\":\"qwen2.5:7b\"}]}",
                                MediaType.APPLICATION_JSON));

        assertTrue(client.isReachable());
    }

    @Test
    void 探活_模型不存在_返回false() {
        server.expect(requestTo(TAGS_URL))
                .andRespond(
                        withSuccess(
                                "{\"models\":[{\"name\":\"llama3:8b\"}]}",
                                MediaType.APPLICATION_JSON));

        assertFalse(client.isReachable());
    }

    @Test
    void 探活_连接异常_返回false且不抛异常() {
        server.expect(requestTo(TAGS_URL))
                .andRespond(
                        request -> {
                            throw new IOException("connection refused");
                        });

        assertFalse(client.isReachable());
    }

    @Test
    void 探活_服务端非2xx_返回false() {
        server.expect(requestTo(TAGS_URL)).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertFalse(client.isReachable());
    }

    @Test
    void 解析增量_正常content() {
        Optional<String> delta =
                OllamaClient.parseDelta(
                        "{\"message\":{\"role\":\"assistant\",\"content\":\"本月\"},\"done\":false}");

        assertEquals(Optional.of("本月"), delta);
    }

    @Test
    void 解析增量_空content返回空() {
        Optional<String> delta =
                OllamaClient.parseDelta(
                        "{\"message\":{\"role\":\"assistant\",\"content\":\"\"},\"done\":false}");

        assertTrue(delta.isEmpty());
    }

    @Test
    void 解析增量_非法行返回空() {
        assertTrue(OllamaClient.parseDelta("not-a-json").isEmpty());
        assertTrue(OllamaClient.parseDelta("").isEmpty());
        assertTrue(OllamaClient.parseDelta(null).isEmpty());
    }

    @Test
    void 解析完成标记() {
        assertTrue(OllamaClient.isDone("{\"done\":true}"));
        assertFalse(OllamaClient.isDone("{\"done\":false}"));
        assertFalse(OllamaClient.isDone("not-a-json"));
        assertFalse(OllamaClient.isDone(null));
    }

    @Test
    void 解析用量_末行含token计数() {
        Optional<OllamaUsage> usage =
                OllamaClient.parseUsage(
                        "{\"done\":true,\"prompt_eval_count\":12,\"eval_count\":34}");

        assertEquals(Optional.of(new OllamaUsage(12, 34)), usage);
    }

    @Test
    void 解析用量_缺字段或非法行返回空() {
        assertTrue(OllamaClient.parseUsage("{\"done\":true}").isEmpty());
        assertTrue(OllamaClient.parseUsage("{\"prompt_eval_count\":12}").isEmpty());
        assertTrue(OllamaClient.parseUsage("not-a-json").isEmpty());
        assertTrue(OllamaClient.parseUsage(null).isEmpty());
    }

    @Test
    void 解析工具调用_arguments为对象() {
        List<OllamaToolCall> calls =
                OllamaClient.parseToolCalls(
                        "{\"message\":{\"role\":\"assistant\",\"content\":\"\",\"tool_calls\":[{\"function\":"
                                + "{\"name\":\"queryExpenseSummary\",\"arguments\":"
                                + "{\"period\":\"2026-11\",\"limit\":10}}}]},\"done\":false}");

        assertEquals(1, calls.size());
        assertEquals("queryExpenseSummary", calls.get(0).name());
        assertEquals(Map.of("period", "2026-11", "limit", 10), calls.get(0).arguments());
    }

    @Test
    void 解析工具调用_arguments为JSON字符串() {
        List<OllamaToolCall> calls =
                OllamaClient.parseToolCalls(
                        "{\"message\":{\"role\":\"assistant\",\"tool_calls\":[{\"function\":"
                                + "{\"name\":\"queryCashflow\",\"arguments\":"
                                + "\"{\\\"period\\\":\\\"2026-11\\\"}\"}}]},\"done\":false}");

        assertEquals(1, calls.size());
        assertEquals("queryCashflow", calls.get(0).name());
        assertEquals(Map.of("period", "2026-11"), calls.get(0).arguments());
    }

    @Test
    void 解析工具调用_无字段或非法行返回空() {
        assertTrue(
                OllamaClient.parseToolCalls(
                                "{\"message\":{\"role\":\"assistant\",\"content\":\"hi\"}}")
                        .isEmpty());
        assertTrue(OllamaClient.parseToolCalls("{\"message\":{\"tool_calls\":[]}}").isEmpty());
        assertTrue(OllamaClient.parseToolCalls("not-a-json").isEmpty());
        assertTrue(OllamaClient.parseToolCalls("").isEmpty());
        assertTrue(OllamaClient.parseToolCalls(null).isEmpty());
    }

    @Test
    void 流式对话_汇总增量与用量() {
        String ndjson =
                "{\"message\":{\"role\":\"assistant\",\"content\":\"本月\"},\"done\":false}\n"
                        + "{\"message\":{\"role\":\"assistant\",\"content\":\"合计100\"},\"done\":false}\n"
                        + "{\"message\":{\"role\":\"assistant\",\"content\":\"\"},\"done\":true,"
                        + "\"prompt_eval_count\":12,\"eval_count\":34}\n";
        server.expect(requestTo(CHAT_URL))
                .andExpect(jsonPath("$.tools").doesNotExist())
                .andRespond(withSuccess(ndjson, MediaType.APPLICATION_NDJSON));

        List<String> deltas = new ArrayList<>();
        OllamaChatResult result =
                client.chatStream(List.of(ChatMessage.user("本月报销多少")), deltas::add);

        assertEquals("本月合计100", result.getContent());
        assertEquals(List.of("本月", "合计100"), deltas);
        assertEquals(12, result.getPromptTokens());
        assertEquals(34, result.getCompletionTokens());
        assertTrue(result.getToolCalls().isEmpty());
        assertTrue(result.getLatencyMs() >= 0L);
    }

    @Test
    void 流式对话_无内容仅工具调用() {
        String ndjson =
                "{\"message\":{\"role\":\"assistant\",\"tool_calls\":[{\"function\":"
                        + "{\"name\":\"queryExpenseSummary\",\"arguments\":{\"period\":\"2026-11\"}}}]},"
                        + "\"done\":false}\n"
                        + "{\"message\":{\"role\":\"assistant\",\"content\":\"\"},\"done\":true,"
                        + "\"prompt_eval_count\":20,\"eval_count\":3}\n";
        server.expect(requestTo(CHAT_URL))
                .andRespond(withSuccess(ndjson, MediaType.APPLICATION_NDJSON));

        List<String> deltas = new ArrayList<>();
        OllamaChatResult result =
                client.chatStream(
                        List.of(ChatMessage.user("本月报销多少"), ChatMessage.tool("{}")),
                        List.of(toolDefinition("queryExpenseSummary")),
                        deltas::add);

        assertEquals("", result.getContent());
        assertTrue(deltas.isEmpty());
        assertEquals(1, result.getToolCalls().size());
        assertEquals("queryExpenseSummary", result.getToolCalls().get(0).name());
        assertEquals(Map.of("period", "2026-11"), result.getToolCalls().get(0).arguments());
        assertEquals(20, result.getPromptTokens());
        assertEquals(3, result.getCompletionTokens());
    }

    @Test
    void 流式对话_携带tools请求体() {
        String ndjson =
                "{\"message\":{\"role\":\"assistant\",\"content\":\"ok\"},\"done\":true,"
                        + "\"prompt_eval_count\":1,\"eval_count\":2}\n";
        server.expect(requestTo(CHAT_URL))
                .andExpect(jsonPath("$.tools[0].type").value("function"))
                .andExpect(jsonPath("$.tools[0].function.name").value("queryProjectSummary"))
                .andRespond(withSuccess(ndjson, MediaType.APPLICATION_NDJSON));

        OllamaChatResult result =
                client.chatStream(
                        List.of(ChatMessage.user("查项目")),
                        List.of(toolDefinition("queryProjectSummary")),
                        null);

        assertEquals("ok", result.getContent());
    }

    /** 构造一个最小可用的工具定义（仅用于验证请求体携带 tools）。 */
    private static Map<String, Object> toolDefinition(String name) {
        Map<String, Object> function = new LinkedHashMap<>(3);
        function.put("name", name);
        function.put("description", "test");
        function.put("parameters", Map.of("type", "object", "properties", Map.of()));
        Map<String, Object> definition = new LinkedHashMap<>(2);
        definition.put("type", "function");
        definition.put("function", function);
        return definition;
    }
}
