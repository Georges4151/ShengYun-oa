package com.ceams.module.iam.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 高危接口 HMAC 签名密钥下发响应负载。
 */
@Data
@AllArgsConstructor
public class SignSecretVO {

    /** HMAC 共享密钥。 */
    private String secret;

    /** 密钥缓存过期时间（epoch 毫秒），客户端据此刷新。 */
    private long expiresAt;
}
