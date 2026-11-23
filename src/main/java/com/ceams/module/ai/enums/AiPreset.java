package com.ceams.module.ai.enums;

import java.util.Locale;

/**
 * 内置 AI 厂商预设：仅提供默认地址与模型名，用户仍可修改为自定义值。
 *
 * <p>{@link #CUSTOM} 的地址与模型均为空，由管理员自行填写。
 */
public enum AiPreset {
    DEEPSEEK(
            "DeepSeek",
            "https://api.deepseek.com/v1",
            "deepseek-chat",
            null),
    DASHSCOPE(
            "阿里云百炼",
            "https://dashscope.aliyuncs.com/compatible-mode/v1",
            "qwen-plus",
            "qwen-vl-max"),
    SILICONFLOW(
            "硅基流动",
            "https://api.siliconflow.cn/v1",
            "Qwen/Qwen2.5-7B-Instruct",
            "Qwen/Qwen2.5-VL-7B-Instruct"),
    OPENAI(
            "OpenAI",
            "https://api.openai.com/v1",
            "gpt-4o-mini",
            "gpt-4o-mini"),
    CUSTOM("自定义", "", "", "");

    private final String displayName;
    private final String baseUrl;
    private final String textModel;
    private final String visionModel;

    AiPreset(String displayName, String baseUrl, String textModel, String visionModel) {
        this.displayName = displayName;
        this.baseUrl = baseUrl;
        this.textModel = textModel;
        this.visionModel = visionModel;
    }

    /** 展示名称。 */
    public String getDisplayName() {
        return displayName;
    }

    /** 默认服务地址（CUSTOM 为空串）。 */
    public String getBaseUrl() {
        return baseUrl;
    }

    /** 默认文本模型（可能为 null，如 DeepSeek 无视觉模型）。 */
    public String getTextModel() {
        return textModel;
    }

    /** 默认视觉模型（可能为 null）。 */
    public String getVisionModel() {
        return visionModel;
    }

    /**
     * 宽松解析：忽略大小写与首尾空白；无法识别返回 {@code null}。
     *
     * @param value 原始字符串
     * @return 匹配的枚举；非法值返回 null
     */
    public static AiPreset from(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return AiPreset.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
