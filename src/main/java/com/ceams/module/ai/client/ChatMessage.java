package com.ceams.module.ai.client;

import java.util.Objects;

/** 发送给 Ollama {@code /api/chat} 的单条对话消息（role/content）。 */
public record ChatMessage(String role, String content) {

    /** 紧凑构造器：强制 role/content 非空，避免拼装请求体时出现 null。 */
    public ChatMessage {
        Objects.requireNonNull(role, "role 不能为空");
        Objects.requireNonNull(content, "content 不能为空");
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

    /** 工具结果消息。 */
    public static ChatMessage tool(String content) {
        return new ChatMessage("tool", content);
    }
}
