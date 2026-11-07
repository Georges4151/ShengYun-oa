package com.ceams.module.ai.controller;

import com.ceams.module.ai.config.AiProperties;
import com.ceams.module.ai.dto.AiChatRequest;
import com.ceams.module.ai.service.AiChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** AI 流式对话接口（SSE：meta / delta / tool / done / error）。 */
@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class AiChatController {

    /** SSE 在配置的对话超时基础上再多留 5 秒余量，避免模型刚好写完却被容器提前断开。 */
    private static final long SSE_TIMEOUT_MARGIN_MILLIS = 5_000L;

    private final AiChatService aiChatService;
    private final AiProperties aiProperties;

    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@Valid @RequestBody AiChatRequest request) {
        long timeoutMillis =
                Math.max(1, aiProperties.getTimeoutSeconds()) * 1000L + SSE_TIMEOUT_MARGIN_MILLIS;
        SseEmitter emitter = new SseEmitter(timeoutMillis);
        aiChatService.chat(request, emitter);
        return emitter;
    }
}
