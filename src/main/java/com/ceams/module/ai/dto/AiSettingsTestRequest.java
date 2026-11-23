package com.ceams.module.ai.dto;

import lombok.Data;

/** 管理员连接测试请求：除 purpose 外均可缺省，缺省时取库中当前值。 */
@Data
public class AiSettingsTestRequest {

    /** TEXT / VISION。 */
    private String purpose;

    /** OLLAMA / OPENAI_COMPAT。 */
    private String provider;

    /** 预设编码。 */
    private String preset;

    /** 服务地址。 */
    private String baseUrl;

    /** 模型名。 */
    private String model;

    /** 超时秒数。 */
    private Integer timeoutSeconds;

    /** 临时 API Key；为空时使用已保存的 Key。 */
    private String apiKey;
}
