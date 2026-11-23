package com.ceams.module.ai.config;

/**
 * AI 配置进程内快照：全局开关 + 两个用途配置。
 *
 * @param enabled 全局开关
 * @param text 文本对话配置
 * @param vision 票据视觉识别配置
 */
public record AiSettingsSnapshot(
        boolean enabled, AiPurposeConfig text, AiPurposeConfig vision) {

    /** 按用途取配置。 */
    public AiPurposeConfig purposeConfig(com.ceams.module.ai.enums.AiPurpose purpose) {
        return purpose == com.ceams.module.ai.enums.AiPurpose.VISION ? vision : text;
    }
}
