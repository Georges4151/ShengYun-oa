package com.ceams.module.ai.client;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Ollama 流式响应中的一次工具调用请求。
 *
 * @param name 工具名（后续必须在 {@code ToolRegistry} 白名单内才能执行）
 * @param arguments 工具参数；Ollama 既可能返回 JSON 对象，也可能返回 JSON 字符串，解析后统一为 Map
 */
public record OllamaToolCall(String name, Map<String, Object> arguments) {

    public OllamaToolCall {
        name = name == null ? "" : name;
        arguments =
                arguments == null
                        ? Map.of()
                        : Collections.unmodifiableMap(new LinkedHashMap<>(arguments));
    }
}
