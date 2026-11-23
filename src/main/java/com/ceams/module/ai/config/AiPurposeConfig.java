package com.ceams.module.ai.config;

import com.ceams.module.ai.enums.AiPreset;
import com.ceams.module.ai.enums.AiProvider;
import com.ceams.module.ai.enums.AiPurpose;
import lombok.Data;

/**
 * 单个用途（TEXT / VISION）的运行时配置快照。
 *
 * <p>由 {@code AiSettingsService} 从数据库加载并解密 API Key 后构造，仅供进程内路由使用；
 * 不直接对外序列化，避免 API Key 泄露。
 */
@Data
public class AiPurposeConfig {

    /** 用途。 */
    private AiPurpose purpose;

    /** 提供商。 */
    private AiProvider provider = AiProvider.OLLAMA;

    /** 预设编码。 */
    private AiPreset preset = AiPreset.CUSTOM;

    /** 服务地址。 */
    private String baseUrl;

    /** API Key 明文（仅进程内使用，禁止日志输出）。 */
    private String apiKey;

    /** 模型名。 */
    private String model;

    /** 请求读取超时（秒）。 */
    private int timeoutSeconds = 120;
}
