package com.ceams.module.ai.service;

import java.util.Map;

/**
 * 对话事件输出端：生产实现写 {@code SseEmitter}，单测实现记录事件，二者共用同一套编排逻辑。
 *
 * <p>事件名与协议严格对应 {@code docs/AI_IMPROVEMENT_PLAN.md} §5：meta / delta / tool / done / error。
 */
public interface AiChatEventSink {

    /** 首包：会话 ID 与模型名。 */
    void meta(String conversationId, String model);

    /** 增量回答文本。 */
    void delta(String text);

    /** 一次工具调用：工具名、参数、返回行数。 */
    void tool(String name, Map<String, Object> args, int rows);

    /** 结束：助手消息 ID（落库失败时为空串）与总耗时。 */
    void done(String messageId, long latencyMs);

    /** 错误：业务码与可展示消息。 */
    void error(String code, String message);

    /** 关闭流（幂等）。 */
    void complete();
}
