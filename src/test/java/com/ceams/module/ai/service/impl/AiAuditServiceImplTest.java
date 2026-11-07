package com.ceams.module.ai.service.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ceams.module.ai.entity.AiConversation;
import com.ceams.module.ai.entity.AiMessage;
import com.ceams.module.ai.mapper.AiConversationMapper;
import com.ceams.module.ai.mapper.AiMessageMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * {@link AiAuditServiceImpl} 审计落库单元测试。
 *
 * <p>MyBatis-Plus 的 {@code insert(...)} 同时存在 {@code T} 与 {@code Collection<T>} 重载， Mockito
 * 必须传入具体类型（如 {@code any(AiConversation.class)}）以消除歧义。
 */
@ExtendWith(MockitoExtension.class)
class AiAuditServiceImplTest {

    @Mock private AiConversationMapper aiConversationMapper;
    @Mock private AiMessageMapper aiMessageMapper;

    @InjectMocks private AiAuditServiceImpl service;

    @BeforeEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void 创建会话_调用insert() {
        service.createConversation("测试会话");

        verify(aiConversationMapper).insert(any(AiConversation.class));
    }

    @Test
    void 创建会话_标题为空时取问题前30字() {
        service.createConversation(null, "一".repeat(40));

        ArgumentCaptor<AiConversation> captor = ArgumentCaptor.forClass(AiConversation.class);
        verify(aiConversationMapper).insert(captor.capture());
        assertEquals("一".repeat(30), captor.getValue().getTitle());
    }

    @Test
    void 创建会话_标题与问题均为空时使用默认标题() {
        service.createConversation(null);

        ArgumentCaptor<AiConversation> captor = ArgumentCaptor.forClass(AiConversation.class);
        verify(aiConversationMapper).insert(captor.capture());
        assertEquals("新会话", captor.getValue().getTitle());
    }

    @Test
    void 追加消息_调用insert() {
        service.appendMessage(100L, "USER", "你好", null, "qwen2.5:7b", 10, 20, 30);

        verify(aiMessageMapper).insert(any(AiMessage.class));
    }

    @Test
    void 创建会话_mapper异常_不向上抛() {
        when(aiConversationMapper.insert(any(AiConversation.class)))
                .thenThrow(new RuntimeException("db down"));

        assertDoesNotThrow(() -> service.createConversation("测试会话"));
        assertNull(service.createConversation("测试会话"));
    }

    @Test
    void 追加消息_mapper异常_不向上抛() {
        when(aiMessageMapper.insert(any(AiMessage.class)))
                .thenThrow(new RuntimeException("db down"));

        assertDoesNotThrow(
                () -> service.appendMessage(100L, "USER", "你好", null, "qwen2.5:7b", 10, 20, 30));
        assertNull(service.appendMessage(100L, "USER", "你好", null, "qwen2.5:7b", 10, 20, 30));
    }
}
