package com.ceams.module.ai.enums;

import java.util.Locale;

/** AI 提供商类型：本地 Ollama 或 OpenAI 兼容互联网接口。 */
public enum AiProvider {
    OLLAMA,
    OPENAI_COMPAT;

    /**
     * 宽松解析：忽略大小写与首尾空白；无法识别返回 {@code null}。
     *
     * @param value 原始字符串
     * @return 匹配的枚举；非法值返回 null
     */
    public static AiProvider from(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return AiProvider.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** 是否为 OpenAI 兼容接口。 */
    public boolean isOpenAiCompatible() {
        return this == OPENAI_COMPAT;
    }
}
