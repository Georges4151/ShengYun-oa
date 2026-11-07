package com.ceams.module.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 本地 Ollama 智能化配置（{@code ceams.ai.*}）。
 *
 * <p>默认关闭：{@link #enabled} 为 false 时状态接口直接返回「未启用」，不发起任何网络请求； 即使开启，Ollama 不可达也只降级为 {@code
 * reachable=false}，不影响主系统。
 */
@Data
@ConfigurationProperties(prefix = "ceams.ai")
public class AiProperties {

    /** 是否启用本地 AI 能力。 */
    private boolean enabled = false;

    /** Ollama 服务地址（默认本机 11434）。 */
    private String baseUrl = "http://localhost:11434";

    /** 使用的模型名（本机已安装 qwen2.5:7b，即指令微调版；可通过 OLLAMA_MODEL 覆盖为 14B 等）。 */
    private String model = "qwen2.5:7b";

    /** 对话请求的读超时（秒）。 */
    private int timeoutSeconds = 120;

    /** 上下文最多携带的历史消息条数（M2 使用）。 */
    private int maxContextMessages = 10;

    /** 工具结果最大返回行数（M2 使用）。 */
    private int maxToolRows = 200;

    /** 是否以流式方式调用 Ollama（默认 true）。 */
    private boolean stream = true;
}
