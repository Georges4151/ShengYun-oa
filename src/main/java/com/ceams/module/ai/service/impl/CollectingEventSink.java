package com.ceams.module.ai.service.impl;

import com.ceams.module.ai.service.AiChatEventSink;
import com.ceams.module.ai.vo.AiToolCallVO;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 非流式对话事件收集器：把编排层发出的 meta / delta / tool / done / error 事件累积起来， 供 {@code
 * AiChatServiceImpl#chatSync} 一次性组装成响应。
 *
 * <p>与 {@link SseEmitterEventSink} 共用同一套编排逻辑，区别只在于事件不落网络而是留在内存里。
 */
public class CollectingEventSink implements AiChatEventSink {

    /** 增量文本按事件顺序拼接。 */
    private final StringBuilder answer = new StringBuilder();

    /** 工具调用按执行顺序记录。 */
    private final List<AiToolCallVO> toolCalls = new ArrayList<>();

    private String conversationId;

    private String model;

    private String messageId;

    private long latencyMs = -1L;

    /** 只保留首个错误，避免后续事件覆盖最根本原因。 */
    private String errorCode;

    private String errorMessage;

    @Override
    public void meta(String conversationId, String model) {
        this.conversationId = conversationId;
        this.model = model;
    }

    @Override
    public void delta(String text) {
        if (text != null && !text.isEmpty()) {
            answer.append(text);
        }
    }

    @Override
    public void tool(String name, Map<String, Object> args, int rows) {
        AiToolCallVO toolCall = new AiToolCallVO();
        toolCall.setName(name);
        toolCall.setArgs(args == null ? Map.of() : args);
        toolCall.setRows(rows);
        toolCalls.add(toolCall);
    }

    @Override
    public void done(String messageId, long latencyMs) {
        this.messageId = messageId;
        this.latencyMs = latencyMs;
    }

    @Override
    public void error(String code, String message) {
        if (errorCode == null) {
            this.errorCode = code;
            this.errorMessage = message;
        }
    }

    /** 同步场景无需关闭流，空实现即可。 */
    @Override
    public void complete() {
        // no-op
    }

    public String getConversationId() {
        return conversationId;
    }

    public String getModel() {
        return model;
    }

    /** 拼接后的完整回答文本。 */
    public String getAnswer() {
        return answer.toString();
    }

    public String getMessageId() {
        return messageId;
    }

    public long getLatencyMs() {
        return latencyMs;
    }

    /** 只读视图，防止调用方改动内部列表。 */
    public List<AiToolCallVO> getToolCalls() {
        return Collections.unmodifiableList(toolCalls);
    }

    /** 编排层是否输出了 error 事件。 */
    public boolean hasError() {
        return errorCode != null;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
