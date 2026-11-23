package com.ceams.module.ai.client;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * AI 对话客户端抽象：屏蔽 Ollama 与 OpenAI 兼容接口的协议差异。
 *
 * <p>上层服务只依赖本接口（由 {@code RoutingAiChatClient} 按当前配置路由到具体实现），
 * 不再直接依赖任何厂商 SDK 或具体客户端。
 */
public interface AiChatClient {

    /** 全局 AI 开关是否开启（路由实现返回数据库/环境配置）。 */
    default boolean isEnabled() {
        return true;
    }

    /** 当前文本模型名（用于落库与展示）。 */
    default String textModel() {
        return "";
    }

    /** 当前视觉模型名。 */
    default String visionModel() {
        return "";
    }

    /** 当前文本对话读取超时（秒）。 */
    default int timeoutSeconds() {
        return 120;
    }

    /** 配置修订号：配置保存后递增，供探活缓存判断是否需要刷新。 */
    default long configRevision() {
        return 0L;
    }

    /** 探活：不可达、鉴权失败、模型缺失等都返回 false，不抛异常。 */
    boolean isReachable();

    /** 探活并返回中文原因。 */
    AiProbeResult probe();

    /**
     * 流式对话（含工具定义）。
     *
     * @param messages 完整上下文
     * @param tools 工具定义（OpenAI/Ollama 通用的 type=function 结构）；null 或空表示不携带
     * @param onDelta 增量文本回调，允许为 null
     * @return 汇总结果（文本、工具调用、token 用量、耗时）
     */
    OllamaChatResult chatStream(
            List<ChatMessage> messages,
            List<Map<String, Object>> tools,
            Consumer<String> onDelta);

    /** 流式对话（无工具）。 */
    default OllamaChatResult chatStream(List<ChatMessage> messages, Consumer<String> onDelta) {
        return chatStream(messages, List.of(), onDelta);
    }

    /**
     * 带图对话（非流式）。
     *
     * @param model 视觉模型名
     * @param prompt 提示词
     * @param images base64 图片列表（不含 data: 前缀）
     * @return 模型输出文本
     */
    String chatWithImages(String model, String prompt, List<String> images);
}
