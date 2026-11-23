package com.ceams.module.ai.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.module.ai.client.ChatMessage;
import com.ceams.module.ai.client.OllamaChatResult;
import com.ceams.module.ai.client.AiChatClient;
import com.ceams.module.ai.client.OllamaToolCall;
import com.ceams.module.ai.config.AiProperties;
import com.ceams.module.ai.dto.AiChatRequest;
import com.ceams.module.ai.entity.AiConversation;
import com.ceams.module.ai.mapper.AiConversationMapper;
import com.ceams.module.ai.mapper.AiMessageMapper;
import com.ceams.module.ai.service.AiAuditService;
import com.ceams.module.ai.service.AiChatEventSink;
import com.ceams.module.ai.tool.ReadOnlyTool;
import com.ceams.module.ai.tool.ToolRegistry;
import com.ceams.module.ai.tool.ToolResult;
import com.ceams.module.ai.vo.AiChatSyncVO;
import com.ceams.module.ai.vo.AiToolCallVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.OngoingStubbing;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * {@link AiChatServiceImpl} 单元测试：全程 Mockito，不启动 Spring 容器、不连库、不依赖 Ollama。
 *
 * <p>直接驱动包级方法 {@code process(...)}，用记录型 {@link AiChatEventSink} 断言 SSE 事件。
 */
@ExtendWith(MockitoExtension.class)
class AiChatServiceImplTest {

    private static final Long USER_ID = 1001L;
    private static final Long OTHER_USER_ID = 2002L;
    private static final Long CONVERSATION_ID = 3001L;
    private static final Long ASSISTANT_MESSAGE_ID = 5001L;
    private static final String QUESTION = "本月报销一共多少钱";

    /** 与 {@code AiChatServiceImpl#UNGROUNDED_ANSWER} 保持一致的预期安全提示。 */
    private static final String UNGROUNDED_HINT =
            "为了确保金额准确，我需要先按你问的期间查询数据。请把期间说明清楚（例如「2026-10 的报销合计是多少」）后再试。";

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");

    /** 固定「今天」= 2026-11-07（周五），避免提示词断言随真实日期漂移。 */
    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-11-07T02:00:00Z"), BUSINESS_ZONE);

    @Mock private AiChatClient aiChatClient;
    @Mock private ToolRegistry toolRegistry;
    @Mock private AiAuditService aiAuditService;
    @Mock private AiConversationMapper conversationMapper;
    @Mock private AiMessageMapper messageMapper;

    private AiProperties properties;
    private AiChatServiceImpl service;

    @BeforeEach
    void setUp() {
        properties = new AiProperties();
        properties.setEnabled(true);
        properties.setModel("qwen2.5:7b");
        properties.setMaxContextMessages(10);
        lenient().when(aiChatClient.isEnabled()).thenReturn(true);
        lenient().when(aiChatClient.textModel()).thenReturn("qwen2.5:7b");
        service =
                new AiChatServiceImpl(
                        properties,
                        aiChatClient,
                        toolRegistry,
                        aiAuditService,
                        conversationMapper,
                        messageMapper,
                        new ObjectMapper(),
                        Runnable::run,
                        FIXED_CLOCK);
        login(USER_ID, "STAFF");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @SuppressWarnings("unchecked")
    void 系统提示注入服务端当前日期与当前月份() {
        stubNewConversation();
        stubChatSequence(answer("本月报销合计 3000 元", 10, 20));

        RecordingSink sink = new RecordingSink();
        service.process(request(QUESTION), sink);

        ArgumentCaptor<List<ChatMessage>> captor = ArgumentCaptor.forClass(List.class);
        verify(aiChatClient, times(1)).chatStream(captor.capture(), anyList(), any());
        List<ChatMessage> messages = captor.getValue();
        assertEquals("system", messages.get(0).role());
        String prompt = messages.get(0).content();
        assertTrue(prompt.contains("2026-11-07"));
        assertTrue(prompt.contains("当前月份是 2026-11"));
        assertTrue(prompt.contains("2026-10"));
        assertTrue(prompt.contains("不能编造"));
    }

    @Test
    void 无工具单轮回答_发出meta_delta_done() {
        stubNewConversation();
        stubChatSequence(answer("本月暂无报销记录。", 10, 20));

        RecordingSink sink = new RecordingSink();
        service.process(request(QUESTION), sink);

        assertTrue(sink.completed);
        assertEquals(String.valueOf(CONVERSATION_ID), sink.metaConversationId);
        assertEquals("qwen2.5:7b", sink.metaModel);
        assertEquals("本月暂无报销记录。", sink.deltas.toString());
        assertTrue(sink.toolEvents.isEmpty());
        assertNull(sink.errorCode);
        assertEquals(String.valueOf(ASSISTANT_MESSAGE_ID), sink.doneMessageId);
        verify(aiChatClient, times(1)).chatStream(anyList(), anyList(), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void 一轮工具调用后作答_执行工具并回灌tool结果() {
        stubNewConversation();
        ReadOnlyTool tool = org.mockito.Mockito.mock(ReadOnlyTool.class);
        when(toolRegistry.find("queryExpenseSummary")).thenReturn(Optional.of(tool));
        when(tool.execute(anyMap()))
                .thenReturn(new ToolResult(List.of(Map.of("totalAmount", 3000)), 1, false));
        stubChatSequence(
                toolCall("queryExpenseSummary", Map.of("period", "2026-11"), 9, 5),
                answer("本月报销合计 3000 元", 12, 30));

        RecordingSink sink = new RecordingSink();
        service.process(request(QUESTION), sink);

        assertEquals(1, sink.toolEvents.size());
        assertEquals("queryExpenseSummary", sink.toolEvents.get(0).name());
        assertEquals(1, sink.toolEvents.get(0).rows());
        assertNull(sink.errorCode);
        assertEquals(String.valueOf(ASSISTANT_MESSAGE_ID), sink.doneMessageId);

        ArgumentCaptor<List<ChatMessage>> captor = ArgumentCaptor.forClass(List.class);
        verify(aiChatClient, times(2)).chatStream(captor.capture(), anyList(), any());
        List<ChatMessage> secondRound = captor.getAllValues().get(1);
        assertTrue(
                secondRound.stream()
                        .anyMatch(
                                message ->
                                        "assistant".equals(message.role())
                                                && !message.toolCalls().isEmpty()));
        assertTrue(
                secondRound.stream()
                        .anyMatch(
                                message ->
                                        "tool".equals(message.role())
                                                && message.content().contains("rows")));
    }

    @Test
    void 工具抛业务异常_仍能继续作答() {
        stubNewConversation();
        ReadOnlyTool tool = org.mockito.Mockito.mock(ReadOnlyTool.class);
        when(toolRegistry.find("queryCashflow")).thenReturn(Optional.of(tool));
        when(tool.execute(anyMap()))
                .thenThrow(BusinessException.of(ErrorCode.VALID_ILLEGAL_ARGUMENT, "期间格式非法"));
        stubChatSequence(
                toolCall("queryCashflow", Map.of("period", "bad"), 8, 4),
                answer("无法获取资金数据，请检查查询期间", 10, 12));

        RecordingSink sink = new RecordingSink();
        service.process(request(QUESTION), sink);

        assertNull(sink.errorCode);
        assertEquals(String.valueOf(ASSISTANT_MESSAGE_ID), sink.doneMessageId);
        assertEquals(1, sink.toolEvents.size());
        assertEquals(0, sink.toolEvents.get(0).rows());

        ArgumentCaptor<List<ChatMessage>> captor = ArgumentCaptor.forClass(List.class);
        verify(aiChatClient, times(2)).chatStream(captor.capture(), anyList(), any());
        assertTrue(
                captor.getAllValues().get(1).stream()
                        .anyMatch(
                                message ->
                                        "tool".equals(message.role())
                                                && message.content().contains("查询失败")));
    }

    @Test
    void 未知工具名_忽略且不执行() {
        stubNewConversation();
        when(toolRegistry.find(anyString())).thenReturn(Optional.empty());
        stubChatSequence(toolCall("queryUnknown", Map.of(), 7, 3), answer("暂无相关数据", 9, 10));

        RecordingSink sink = new RecordingSink();
        service.process(request(QUESTION), sink);

        assertTrue(sink.toolEvents.isEmpty());
        assertNull(sink.errorCode);
        assertEquals(String.valueOf(ASSISTANT_MESSAGE_ID), sink.doneMessageId);
        verify(aiChatClient, times(2)).chatStream(anyList(), anyList(), any());
    }

    @Test
    void 未启用时_同步抛业务异常() {
        when(aiChatClient.isEnabled()).thenReturn(false);

        assertThrows(
                BusinessException.class,
                () -> service.chat(request(QUESTION), new SseEmitter(1_000L)));
        verify(aiChatClient, never()).chatStream(anyList(), anyList(), any());
    }

    @Test
    void 会话不属于当前用户_返回BIZ_NOT_FOUND且不调用模型() {
        AiConversation conversation = new AiConversation();
        conversation.setId(7001L);
        conversation.setUserId(OTHER_USER_ID);
        when(conversationMapper.selectById(7001L)).thenReturn(conversation);

        AiChatRequest request = request(QUESTION);
        request.setConversationId("7001");
        RecordingSink sink = new RecordingSink();
        service.process(request, sink);

        assertEquals(ErrorCode.BIZ_NOT_FOUND.name(), sink.errorCode);
        assertNull(sink.metaConversationId);
        assertTrue(sink.completed);
        verify(aiChatClient, never()).chatStream(anyList(), anyList(), any());
    }

    @Test
    void 助手消息落库失败_不影响done事件() {
        stubNewConversation();
        when(aiAuditService.appendMessage(
                        anyLong(), anyString(), anyString(), any(), any(), any(), any(), any()))
                .thenReturn(ASSISTANT_MESSAGE_ID)
                .thenThrow(new RuntimeException("db down"));
        stubChatSequence(answer("本月报销合计 3000 元", 10, 20));

        RecordingSink sink = new RecordingSink();
        service.process(request(QUESTION), sink);

        assertNull(sink.errorCode);
        assertTrue(sink.completed);
        assertEquals("", sink.doneMessageId);
        assertTrue(sink.doneLatencyMs >= 0L);
    }

    @Test
    void 数据类问题未调用工具且回答含金额_输出安全提示() {
        stubNewConversation();
        stubChatSequence(answer("上月（2026-10）的报销总额为 53,800 元。", 10, 20));

        RecordingSink sink = new RecordingSink();
        service.process(request("上月报销一共多少钱？"), sink);

        assertEquals(UNGROUNDED_HINT, sink.deltas.toString());
        assertFalse(sink.deltas.toString().contains("53,800"));
        assertTrue(sink.toolEvents.isEmpty());
        assertNull(sink.errorCode);
        assertTrue(sink.completed);
        assertEquals(String.valueOf(ASSISTANT_MESSAGE_ID), sink.doneMessageId);
        verify(aiChatClient, times(1)).chatStream(anyList(), anyList(), any());

        ArgumentCaptor<String> contentCaptor = ArgumentCaptor.forClass(String.class);
        verify(aiAuditService, times(2))
                .appendMessage(
                        anyLong(),
                        anyString(),
                        contentCaptor.capture(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any());
        assertEquals(UNGROUNDED_HINT, contentCaptor.getAllValues().get(1));
    }

    @Test
    void 数据类问题未调用工具但无金额_正常输出() {
        stubNewConversation();
        stubChatSequence(answer("未查询到相关数据。", 10, 20));

        RecordingSink sink = new RecordingSink();
        service.process(request("上月报销一共多少钱？"), sink);

        assertEquals("未查询到相关数据。", sink.deltas.toString());
        assertTrue(sink.toolEvents.isEmpty());
        assertNull(sink.errorCode);
        assertTrue(sink.completed);
        assertEquals(String.valueOf(ASSISTANT_MESSAGE_ID), sink.doneMessageId);
    }

    @Test
    void 非数据类问题未调用工具_正常流式输出() {
        stubNewConversation();
        stubChatSequence(answer("我可以帮你查询报销、项目、预算等数据，请说明想了解的期间。", 10, 20));

        RecordingSink sink = new RecordingSink();
        service.process(request("你好，你能帮我做什么？"), sink);

        assertEquals("我可以帮你查询报销、项目、预算等数据，请说明想了解的期间。", sink.deltas.toString());
        assertTrue(sink.toolEvents.isEmpty());
        assertNull(sink.errorCode);
        assertTrue(sink.completed);
        assertEquals(String.valueOf(ASSISTANT_MESSAGE_ID), sink.doneMessageId);
    }

    // ---------- 非流式（chatSync） ----------

    @Test
    void 同步对话_单轮无工具_一次性返回完整回答与字符串化ID() throws Exception {
        stubNewConversation();
        stubChatSequence(answer("本月暂无报销记录。", 10, 20));

        AiChatSyncVO vo = service.chatSync(request(QUESTION));

        assertEquals("本月暂无报销记录。", vo.getAnswer());
        assertEquals(CONVERSATION_ID, vo.getConversationId());
        assertEquals(ASSISTANT_MESSAGE_ID, vo.getMessageId());
        assertEquals("qwen2.5:7b", vo.getModel());
        assertTrue(vo.getLatencyMs() >= 0L);
        assertTrue(vo.getToolCalls().isEmpty());

        String json = new ObjectMapper().writeValueAsString(vo);
        assertTrue(json.contains("\"conversationId\":\"" + CONVERSATION_ID + "\""));
        assertTrue(json.contains("\"messageId\":\"" + ASSISTANT_MESSAGE_ID + "\""));
    }

    @Test
    void 同步对话_包含工具调用_返回toolCalls明细() {
        stubNewConversation();
        ReadOnlyTool tool = org.mockito.Mockito.mock(ReadOnlyTool.class);
        when(toolRegistry.find("queryExpenseSummary")).thenReturn(Optional.of(tool));
        when(tool.execute(anyMap()))
                .thenReturn(new ToolResult(List.of(Map.of("totalAmount", 3000)), 1, false));
        stubChatSequence(
                toolCall("queryExpenseSummary", Map.of("period", "2026-11"), 9, 5),
                answer("本月报销合计 3000 元", 12, 30));

        AiChatSyncVO vo = service.chatSync(request(QUESTION));

        assertEquals("本月报销合计 3000 元", vo.getAnswer());
        assertEquals(1, vo.getToolCalls().size());
        AiToolCallVO toolCall = vo.getToolCalls().get(0);
        assertEquals("queryExpenseSummary", toolCall.getName());
        assertEquals("2026-11", toolCall.getArgs().get("period"));
        assertEquals(1, toolCall.getRows());
        assertTrue(vo.getLatencyMs() >= 0L);
    }

    @Test
    void 同步对话_未启用_抛出业务异常且不调用模型() {
        when(aiChatClient.isEnabled()).thenReturn(false);

        BusinessException exception =
                assertThrows(BusinessException.class, () -> service.chatSync(request(QUESTION)));

        assertEquals(ErrorCode.BIZ_STATE_CONFLICT, exception.getErrorCode());
        verify(aiChatClient, never()).chatStream(anyList(), anyList(), any());
    }

    @Test
    void 同步对话_会话无权访问_抛出BIZ_NOT_FOUND() {
        AiConversation conversation = new AiConversation();
        conversation.setId(7001L);
        conversation.setUserId(OTHER_USER_ID);
        when(conversationMapper.selectById(7001L)).thenReturn(conversation);

        BusinessException exception =
                assertThrows(
                        BusinessException.class, () -> service.chatSync(request(QUESTION, 7001L)));

        assertEquals(ErrorCode.BIZ_NOT_FOUND, exception.getErrorCode());
        verify(aiChatClient, never()).chatStream(anyList(), anyList(), any());
    }

    // ---------- 测试支撑 ----------

    /** 新建会话 + 空历史 + 消息落库成功。 */
    private void stubNewConversation() {
        when(aiAuditService.createConversation(anyString())).thenReturn(CONVERSATION_ID);
        when(aiAuditService.appendMessage(
                        anyLong(), anyString(), anyString(), any(), any(), any(), any(), any()))
                .thenReturn(ASSISTANT_MESSAGE_ID);
        when(messageMapper.selectPage(any(), any())).thenReturn(new Page<>());
    }

    /** 依次返回多个模型结果，并模拟真实客户端的增量回调。 */
    @SafeVarargs
    private final void stubChatSequence(OllamaChatResult... results) {
        OngoingStubbing<OllamaChatResult> stubbing =
                when(aiChatClient.chatStream(anyList(), anyList(), any()));
        for (OllamaChatResult result : results) {
            stubbing =
                    stubbing.thenAnswer(
                            invocation -> {
                                Consumer<String> onDelta = invocation.getArgument(2);
                                if (onDelta != null
                                        && result.getContent() != null
                                        && !result.getContent().isEmpty()) {
                                    onDelta.accept(result.getContent());
                                }
                                return result;
                            });
        }
    }

    private static OllamaChatResult answer(String content, int promptTokens, int completionTokens) {
        OllamaChatResult result = new OllamaChatResult();
        result.setContent(content);
        result.setToolCalls(List.of());
        result.setPromptTokens(promptTokens);
        result.setCompletionTokens(completionTokens);
        return result;
    }

    private static OllamaChatResult toolCall(
            String name, Map<String, Object> args, int promptTokens, int completionTokens) {
        OllamaChatResult result = new OllamaChatResult();
        result.setContent("");
        result.setToolCalls(List.of(new OllamaToolCall(name, args)));
        result.setPromptTokens(promptTokens);
        result.setCompletionTokens(completionTokens);
        return result;
    }

    private static AiChatRequest request(String message) {
        AiChatRequest request = new AiChatRequest();
        request.setMessage(message);
        return request;
    }

    private static AiChatRequest request(String message, Long conversationId) {
        AiChatRequest request = request(message);
        request.setConversationId(String.valueOf(conversationId));
        return request;
    }

    private static void login(Long userId, String... roles) {
        List<SimpleGrantedAuthority> authorities =
                Arrays.stream(roles)
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                        .toList();
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken(userId, null, authorities));
    }

    /** 记录型事件输出端。 */
    private static final class RecordingSink implements AiChatEventSink {

        private String metaConversationId;
        private String metaModel;
        private final StringBuilder deltas = new StringBuilder();
        private final List<ToolEvent> toolEvents = new ArrayList<>();
        private String doneMessageId;
        private long doneLatencyMs = -1L;
        private String errorCode;
        private String errorMessage;
        private boolean completed;

        @Override
        public void meta(String conversationId, String model) {
            this.metaConversationId = conversationId;
            this.metaModel = model;
        }

        @Override
        public void delta(String text) {
            deltas.append(text);
        }

        @Override
        public void tool(String name, Map<String, Object> args, int rows) {
            toolEvents.add(new ToolEvent(name, args, rows));
        }

        @Override
        public void done(String messageId, long latencyMs) {
            this.doneMessageId = messageId;
            this.doneLatencyMs = latencyMs;
        }

        @Override
        public void error(String code, String message) {
            this.errorCode = code;
            this.errorMessage = message;
        }

        @Override
        public void complete() {
            this.completed = true;
        }

        private record ToolEvent(String name, Map<String, Object> args, int rows) {}
    }
}
