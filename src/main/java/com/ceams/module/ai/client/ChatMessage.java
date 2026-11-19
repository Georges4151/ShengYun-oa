package com.ceams.module.ai.client;

import java.util.List;
import java.util.Objects;

/**
 * 发送给 Ollama {@code /api/chat} 的单条对话消息（role/content，助手消息可携带 tool_calls）。
 *
 * @param role 角色：system/user/assistant/tool
 * @param content 消息文本
 * @param toolCalls 助手消息请求的工具调用（仅 assistant 角色使用，其余为空）
 */
public record ChatMessage(String role, String content, List<OllamaToolCall> toolCalls) {

    /** 紧凑构造器：role 非空，content 缺省为空串，toolCalls 缺省为空列表。 */
    public ChatMessage {
        Objects.requireNonNull(role, "role 不能为空");
        content = content == null ? "" : content;
        toolCalls = toolCalls == null ? List.of() : List.copyOf(toolCalls);
    }

    /** 兼容旧调用方的两参构造器。 */
    public ChatMessage(String role, String content) {
        this(role, content, List.of());
    }

    /** 系统提示消息。 */
    public static ChatMessage system(String content) {
        return new ChatMessage("system", content);
    }

    /** 用户消息。 */
    public static ChatMessage user(String content) {
        return new ChatMessage("user", content);
    }

    /** 助手消息。 */
    public static ChatMessage assistant(String content) {
        return new ChatMessage("assistant", content);
    }

    /** 携带工具调用的助手消息（回灌给模型以衔接后续 tool 结果）。 */
    public static ChatMessage assistantWithToolCalls(
            String content, List<OllamaToolCall> toolCalls) {
        return new ChatMessage("assistant", content, toolCalls);
    }

    /** 工具结果消息。 */
    public static ChatMessage tool(String content) {
        return new ChatMessage("tool", content);
    }
}
