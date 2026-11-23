package com.ceams.module.ai.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ceams.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** AI 提供商配置（sys_ai_provider）：TEXT / VISION 各一行。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_ai_provider")
public class SysAiProvider extends BaseEntity {

    /** 用途：TEXT / VISION（唯一）。 */
    private String purpose;

    /** 提供商：OLLAMA / OPENAI_COMPAT。 */
    private String provider;

    /** 内置预设编码。 */
    private String preset;

    /** 服务地址（仅允许 http/https）。 */
    private String baseUrl;

    /** API Key 密文（AES-256-GCM，Base64）。 */
    private String apiKeyCipher;

    /** 模型名。 */
    private String model;

    /** 请求读取超时（秒）。 */
    private Integer timeoutSeconds;
}
