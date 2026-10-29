package com.ceams.common.sign;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 签名密钥解析器（接口）。
 *
 * <p>签名校验已由「全局共享密钥」切换为「按设备 AppSecret」校验：请求必须携带 {@code X-Device-Id}
 * 设备标识，本接口负责按请求解析出该设备的签名密钥（AppSecret）。 接口留在 {@code common}，具体实现放在业务域（iam），避免 common 反向依赖 module。
 */
public interface SignSecretResolver {

    /**
     * 按当前请求解析签名密钥。
     *
     * @param request 当前 HTTP 请求
     * @return 设备 AppSecret（明文，由实现层保证已解密）
     */
    String resolve(HttpServletRequest request);
}
