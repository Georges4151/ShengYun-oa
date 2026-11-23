package com.ceams.module.ai.controller;

import com.ceams.common.response.ApiResponse;
import com.ceams.module.ai.client.AiChatClient;
import com.ceams.module.ai.dto.AiChatRequest;
import com.ceams.module.ai.service.AiChatService;
import com.ceams.module.ai.vo.AiChatSyncVO;
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
    private final AiChatClient aiChatClient;

    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@Valid @RequestBody AiChatRequest request) {
        long timeoutMillis =
                Math.max(1, aiChatClient.timeoutSeconds()) * 1000L + SSE_TIMEOUT_MARGIN_MILLIS;
        SseEmitter emitter = new SseEmitter(timeoutMillis);
        aiChatService.chat(request, emitter);
        return emitter;
    }

    /** 非流式对话：小程序端一次性拿到完整回答（默认 {@code application/json}）。 */
    @PostMapping("/chat/sync")
    public ApiResponse<AiChatSyncVO> chatSync(@Valid @RequestBody AiChatRequest request) {
        return ApiResponse.success(aiChatService.chatSync(request));
    }
}
