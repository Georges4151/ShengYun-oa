package com.ceams.module.ai.client;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/** 一次 Ollama 流式对话的结果与用量统计。 */
@Data
public class OllamaChatResult {

    /** 拼接后的完整回答文本。 */
    private String content = "";

    /** 模型请求的工具调用（无工具调用时为空列表）。 */
    private List<OllamaToolCall> toolCalls = new ArrayList<>();

    /** 提示词 token 数（来自 done 行的 prompt_eval_count）。 */
    private int promptTokens;

    /** 生成 token 数（来自 done 行的 eval_count）。 */
    private int completionTokens;

    /** 整个调用耗时（毫秒）。 */
    private long latencyMs;
}
