package com.ceams.common.sign;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 签名共享密钥解析器。
 *
 * <p>从配置 {@code ceams.sign.secret} 读取 HMAC 共享密钥。生产环境必须通过环境变量覆盖，
 * 严禁把真实密钥硬编码到仓库。</p>
 */
@Component
public class SignSecretResolver {

    @Value("${ceams.sign.secret}")
    private String secret;

    /** 返回当前生效的 HMAC 共享密钥。 */
    public String resolve() {
        return secret;
    }
}
