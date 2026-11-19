package com.ceams.module.ai.service.impl;

import com.ceams.common.security.SecurityUtils;
import com.ceams.module.ai.entity.AiConversation;
import com.ceams.module.ai.entity.AiMessage;
import com.ceams.module.ai.enums.AiMessageRole;
import com.ceams.module.ai.mapper.AiConversationMapper;
import com.ceams.module.ai.mapper.AiMessageMapper;
import com.ceams.module.ai.service.AiAuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * AI 审计落库实现。
 *
 * <p>{@code user_id / create_by} 取自当前安全上下文，未认证时用 0（系统）；任何落库异常都在本方法内兜底， 仅记 warn 日志并返回 null。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiAuditServiceImpl implements AiAuditService {

    /** ai_conversation.title 列宽。 */
    private static final int TITLE_MAX_LENGTH = 200;

    /** 标题为空时，取问题前 N 个字符生成默认标题。 */
    private static final int TITLE_FROM_QUESTION_LENGTH = 30;

    /** 缺少可用标题与问题时使用的兜底标题。 */
    private static final String DEFAULT_TITLE = "新会话";

    /** ai_message.model 列宽。 */
    private static final int MODEL_MAX_LENGTH = 80;

    private final AiConversationMapper aiConversationMapper;
    private final AiMessageMapper aiMessageMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createConversation(String title, String question) {
        Long userId = SecurityUtils.getCurrentUserIdOrDefault();
        try {
            AiConversation conversation = new AiConversation();
            conversation.setUserId(userId);
            conversation.setTitle(resolveTitle(title, question));
            conversation.setCreateBy(userId);
            conversation.setUpdateBy(userId);
            aiConversationMapper.insert(conversation);
            return conversation.getId();
        } catch (RuntimeException e) {
            log.warn("AI 会话审计落库失败，title={}：{}", title, e.getMessage());
            return null;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long appendMessage(
            Long conversationId,
            String role,
            String content,
            String toolCalls,
            String model,
            Integer promptTokens,
            Integer completionTokens,
            Integer latencyMs) {
        if (conversationId == null) {
            log.warn("AI 消息审计缺少 conversationId，跳过落库");
            return null;
        }
        Long userId = SecurityUtils.getCurrentUserIdOrDefault();
        try {
            AiMessage message = new AiMessage();
            message.setConversationId(conversationId);
            message.setRole(AiMessageRole.fromCode(role));
            message.setContent(content);
            message.setToolCalls(toolCalls);
            message.setModel(truncate(model, MODEL_MAX_LENGTH));
            message.setPromptTokens(promptTokens);
            message.setCompletionTokens(completionTokens);
            message.setLatencyMs(latencyMs);
            message.setCreateBy(userId);
            message.setUpdateBy(userId);
            aiMessageMapper.insert(message);
            return message.getId();
        } catch (RuntimeException e) {
            log.warn("AI 消息审计落库失败，conversationId={}：{}", conversationId, e.getMessage());
            return null;
        }
    }

    /** 标题优先级：显式标题 > 问题前 30 字 > 默认标题，并统一裁剪到列宽。 */
    private static String resolveTitle(String title, String question) {
        if (StringUtils.hasText(title)) {
            return truncate(title.trim(), TITLE_MAX_LENGTH);
        }
        if (StringUtils.hasText(question)) {
            return truncate(question.trim(), TITLE_FROM_QUESTION_LENGTH);
        }
        return DEFAULT_TITLE;
    }

    private static String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
