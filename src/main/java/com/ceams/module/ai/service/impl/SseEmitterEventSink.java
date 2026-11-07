package com.ceams.module.ai.service.impl;

import com.ceams.module.ai.service.AiChatEventSink;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * {@link SseEmitter} 事件输出端。
 *
 * <p>所有 {@code data} 都是 Jackson 紧凑序列化后的单行 JSON（不换行），以 {@code text/plain} 写入，避免 {@code
 * application/json} 消息转换器把 JSON 字符串再次加引号。客户端断开导致的写入异常只记 debug，不打断编排。
 */
@Slf4j
@RequiredArgsConstructor
public class SseEmitterEventSink implements AiChatEventSink {

    private final SseEmitter emitter;
    private final ObjectMapper objectMapper;
    private final AtomicBoolean completed = new AtomicBoolean(false);

    @Override
    public void meta(String conversationId, String model) {
        Map<String, Object> data = new LinkedHashMap<>(2);
        data.put("conversationId", conversationId);
        data.put("model", model);
        send("meta", data);
    }

    @Override
    public void delta(String text) {
        Map<String, Object> data = new LinkedHashMap<>(1);
        data.put("text", text);
        send("delta", data);
    }

    @Override
    public void tool(String name, Map<String, Object> args, int rows) {
        Map<String, Object> data = new LinkedHashMap<>(3);
        data.put("name", name);
        data.put("args", args == null ? Map.of() : args);
        data.put("rows", rows);
        send("tool", data);
    }

    @Override
    public void done(String messageId, long latencyMs) {
        Map<String, Object> data = new LinkedHashMap<>(2);
        data.put("messageId", messageId == null ? "" : messageId);
        data.put("latencyMs", latencyMs);
        send("done", data);
    }

    @Override
    public void error(String code, String message) {
        Map<String, Object> data = new LinkedHashMap<>(2);
        data.put("code", code);
        data.put("message", message);
        send("error", data);
    }

    @Override
    public void complete() {
        if (!completed.compareAndSet(false, true)) {
            return;
        }
        try {
            emitter.complete();
        } catch (RuntimeException e) {
            // 客户端已断开时容器可能已关闭异步上下文，这里只降级为 debug。
            log.debug("关闭 SSE 失败：{}", e.getMessage());
        }
    }

    private void send(String event, Map<String, Object> data) {
        try {
            String json = objectMapper.writeValueAsString(data);
            emitter.send(SseEmitter.event().name(event).data(json, MediaType.TEXT_PLAIN));
        } catch (IOException | IllegalStateException e) {
            log.debug("SSE 写入失败（客户端可能已断开），event={}：{}", event, e.getMessage());
        }
    }
}
