package com.ceams.module.ai.service;

/**
 * AI 审计落库服务。
 *
 * <p>审计写入失败只记 warn 日志、不向上抛异常，避免阻断业务主流程。未来 M2 的 Controller 只依赖本服务， 不直接引用 Mapper。
 */
public interface AiAuditService {

    /**
     * 创建会话并返回会话 ID。
     *
     * @param title 期望标题，可为空；为空时用 {@code question} 前 30 字截断生成
     * @param question 本次用户问题，可为空
     * @return 会话 ID；落库失败返回 null
     */
    Long createConversation(String title, String question);

    /**
     * 创建会话（仅提供问题，标题自动按前 30 字生成）。
     *
     * @param question 本次用户问题
     * @return 会话 ID；落库失败返回 null
     */
    default Long createConversation(String question) {
        return createConversation(null, question);
    }

    /** 追加一条消息并返回消息 ID；落库失败返回 null。 */
    Long appendMessage(
            Long conversationId,
            String role,
            String content,
            String toolCalls,
            String model,
            Integer promptTokens,
            Integer completionTokens,
            Integer latencyMs);
}
