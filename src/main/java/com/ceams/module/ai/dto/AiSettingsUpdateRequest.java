package com.ceams.module.ai.dto;

import lombok.Data;

/** 管理员更新 AI 配置请求。 */
@Data
public class AiSettingsUpdateRequest {

    /** 全局开关。 */
    private Boolean enabled;

    /** 文本对话配置。 */
    private ProviderSettings text;

    /** 票据视觉识别配置。 */
    private ProviderSettings vision;

    /** 单个用途配置。 */
    @Data
    public static class ProviderSettings {

        /** OLLAMA / OPENAI_COMPAT。 */
        private String provider;

        /** DEEPSEEK / DASHSCOPE / SILICONFLOW / OPENAI / CUSTOM。 */
        private String preset;

        /** 服务地址，仅允许 http/https。 */
        private String baseUrl;

        /** 模型名。 */
        private String model;

        /** 超时秒数（10~600）。 */
        private Integer timeoutSeconds;

        /**
         * API Key。
         *
         * <p>字段缺失或 null：保持原值；空字符串：清空；非空：更新。
         */
        private String apiKey;
    }
}
