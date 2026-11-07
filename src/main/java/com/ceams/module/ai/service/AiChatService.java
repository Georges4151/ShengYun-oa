package com.ceams.module.ai.service;

import com.ceams.module.ai.dto.AiChatRequest;
import com.ceams.module.ai.vo.AiChatSyncVO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** AI 对话编排服务：Web 端走 SSE，小程序走一次性同步返回。 */
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

    /**
     * 执行一次非流式对话，阻塞直到整条回答生成完毕。
     *
     * <p>与 {@link #chat} 共用同一套会话校验、工具调用、护栏与落库逻辑；开关关闭、会话无权访问、模型调用失败等情况 直接抛出{@link
     * com.ceams.common.exception.BusinessException}，由全局异常处理转换为统一 JSON。
     *
     * @param request 对话请求
     * @return 一次性返回的完整回答
     */
    AiChatSyncVO chatSync(AiChatRequest request);
}
