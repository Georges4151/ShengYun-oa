package com.ceams.module.ai.vo;

import lombok.Data;

/** 单个用途的 AI 配置展示对象（API Key 仅返回掩码）。 */
@Data
public class AiProviderSettingsVO {

    /** OLLAMA / OPENAI_COMPAT。 */
    private String provider;

    /** 预设编码。 */
    private String preset;

    /** 服务地址。 */
    private String baseUrl;

    /** 模型名。 */
    private String model;

    /** 超时秒数。 */
    private int timeoutSeconds;

    /** 是否已设置 API Key。 */
    private boolean apiKeySet;

    /** 掩码后的 API Key；OLLAMA 或未设置时为 null。 */
    private String apiKeyMasked;
}
