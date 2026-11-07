package com.ceams.module.ai.vo;

import lombok.Data;

/** AI 服务状态视图对象（{@code GET /api/v1/ai/status}）。 */
@Data
public class AiStatusVO {

    /** 功能开关是否开启（{@code ceams.ai.enabled}）。 */
    private boolean enabled;

    /** 当前配置的模型名。 */
    private String model;

    /** Ollama 是否可达且模型已就绪。 */
    private boolean reachable;

    /** 探活耗时（毫秒）；未启用时为 0。 */
    private long latencyMs;
}
