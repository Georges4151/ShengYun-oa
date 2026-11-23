package com.ceams.module.ai.enums;

import java.util.Locale;

/** AI 配置用途：文本对话与票据视觉识别分别独立配置。 */
public enum AiPurpose {
    TEXT,
    VISION;

    /**
     * 宽松解析：忽略大小写与首尾空白；无法识别返回 {@code null}。
     *
     * @param value 原始字符串
     * @return 匹配的枚举；非法值返回 null
     */
    public static AiPurpose from(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return AiPurpose.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
