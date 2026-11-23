package com.ceams.module.ai.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** {@link OpenAiCompatClient} 单元测试：SSE 解析、Bearer 鉴权、tools 请求体与视觉 content 数组。 */
class OpenAiCompatClientTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String BASE_URL = "https://api.example.com/v1";
    private static final String CHAT_URL = BASE_URL + "/chat/completions";
    private static final String MODELS_URL = BASE_URL + "/models";

    private final RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    private final OpenAiCompatClient client =
            new OpenAiCompatClient(
                    BASE_URL,
                    "sk-test",
                    "gpt-4o-mini",
                    true,
                    MAPPER,
                    builder.build(),
                    builder.build());

    @Test
    void 解析SSE文本增量() {
        assertEquals(
                Optional.of("你好"),
                OpenAiCompatClient.parseSseContent(
                        "data: {\"choices\":[{\"delta\":{\"content\":\"你好\"}}]}"));

        assertTrue(OpenAiCompatClient.parseSseContent("data: [DONE]").isEmpty());
        assertTrue(OpenAiCompatClient.parseSseContent(": keep-alive").isEmpty());
        assertTrue(OpenAiCompatClient.parseSseContent("data: not-json").isEmpty());
        assertTrue(OpenAiCompatClient.parseSseContent(null).isEmpty());
    }

    @Test
    void 解析SSE工具调用增量() {
        List<OpenAiCompatClient.ToolCallDelta> deltas =
                OpenAiCompatClient.parseSseToolCalls(
                        "data: {\"choices\":[{\"delta\":{\"tool_calls\":[{\"index\":0,"
                                + "\"id\":\"call_1\",\"function\":{\"name\":\"queryExpense\","
                                + "\"arguments\":\"{\\\"period\\\":\"}}]}}]}");

        assertEquals(1, deltas.size());
        assertEquals(0, deltas.get(0).index());
        assertEquals("call_1", deltas.get(0).id());
        assertEquals("queryExpense", deltas.get(0).name());
        assertEquals("{\"period\":", deltas.get(0).arguments());
    }

    @Test
    void 解析SSE用量() {
        Optional<OllamaUsage> usage =
                OpenAiCompatClient.parseSseUsage(
                        "data: {\"choices\":[],\"usage\":{\"prompt_tokens\":12,\"completion_tokens\":34}}");

        assertEquals(Optional.of(new OllamaUsage(12, 34)), usage);
        assertTrue(OpenAiCompatClient.parseSseUsage("data: {\"choices\":[]}").isEmpty());
    }

    @Test
    void 流式对话_解析增量拼接工具调用并携带Bearer与tools() {
        String sse =
                "data: {\"choices\":[{\"delta\":{\"role\":\"assistant\",\"content\":\"本月\"}}]}\n\n"
                        + "data: {\"choices\":[{\"delta\":{\"tool_calls\":[{\"index\":0,"
                        + "\"id\":\"call_1\",\"function\":{\"name\":\"queryExpense\","
                        + "\"arguments\":\"{\\\"period\\\":\"}}]}}]}\n\n"
                        + "data: {\"choices\":[{\"delta\":{\"tool_calls\":[{\"index\":0,"
                        + "\"function\":{\"arguments\":\"\\\"2026-11\\\"}\"}}]}}]}\n\n"
                        + "data: {\"choices\":[],\"usage\":{\"prompt_tokens\":10,\"completion_tokens\":5}}\n\n"
                        + "data: [DONE]\n\n";
        server.expect(requestTo(CHAT_URL))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer sk-test"))
                .andExpect(jsonPath("$.stream").value(true))
                .andExpect(jsonPath("$.tools[0].function.name").value("queryExpense"))
                .andRespond(withSuccess(sse, MediaType.TEXT_EVENT_STREAM));

        List<String> deltas = new ArrayList<>();
        OllamaChatResult result =
                client.chatStream(
                        List.of(ChatMessage.user("本月报销")),
                        List.of(toolDefinition("queryExpense")),
                        deltas::add);

        assertEquals("本月", result.getContent());
        assertEquals(List.of("本月"), deltas);
        assertEquals(1, result.getToolCalls().size());
        assertEquals("queryExpense", result.getToolCalls().get(0).name());
        assertEquals(Map.of("period", "2026-11"), result.getToolCalls().get(0).arguments());
        assertEquals(10, result.getPromptTokens());
        assertEquals(5, result.getCompletionTokens());
    }

    @Test
    void 带图对话_使用content数组与dataURL() {
        String response =
                "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"金额 100.00\"}}]}";
        server.expect(requestTo(CHAT_URL))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer sk-test"))
                .andExpect(jsonPath("$.stream").value(false))
                .andExpect(jsonPath("$.messages[0].content[0].type").value("text"))
                .andExpect(jsonPath("$.messages[0].content[1].type").value("image_url"))
                .andExpect(
                        jsonPath("$.messages[0].content[1].image_url.url")
                                .value("data:image/jpeg;base64,AAAA"))
                .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));

        String text = client.chatWithImages("qwen-vl-max", "识别票据", List.of("AAAA"));

        assertEquals("金额 100.00", text);
    }

    @Test
    void 探活_鉴权失败返回中文原因() {
        server.expect(requestTo(MODELS_URL))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer sk-test"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        AiProbeResult result = client.probe();

        assertFalse(result.success());
        assertEquals("鉴权失败(401)", result.message());
    }

    @Test
    void 探活_模型列表不包含当前模型_返回模型不可用() {
        server.expect(requestTo(MODELS_URL))
                .andRespond(
                        withSuccess(
                                "{\"data\":[{\"id\":\"other-model\"}]}", MediaType.APPLICATION_JSON));

        AiProbeResult result = client.probe();

        assertFalse(result.success());
        assertTrue(result.message().contains("模型不可用"));
    }

    private static Map<String, Object> toolDefinition(String name) {
        return Map.of(
                "type",
                "function",
                "function",
                Map.of(
                        "name",
                        name,
                        "description",
                        "test",
                        "parameters",
                        Map.of("type", "object", "properties", Map.of())));
    }
}
