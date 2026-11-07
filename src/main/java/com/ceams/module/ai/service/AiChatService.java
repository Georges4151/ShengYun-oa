package com.ceams.module.ai.service;

import com.ceams.module.ai.dto.AiChatRequest;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** AI 流式对话编排服务。 */
public interface AiChatService {

    /**
     * 执行一次流式对话。
     *
     * <p>开关关闭等前置校验会在方法内同步抛出 {@link com.ceams.common.exception.BusinessException}（由全局异常处理 返回
     * JSON，而不是 SSE）；真正耗时部分提交到专用线程池，事件通过 {@code emitter} 输出。
     *
     * @param request 对话请求
     * @param emitter 已创建（含超时）的 SSE 输出端
     */
    void chat(AiChatRequest request, SseEmitter emitter);
}
