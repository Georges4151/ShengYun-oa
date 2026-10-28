package com.ceams.common.sign;

/**
 * nonce 防重放存储抽象。
 *
 * <p>高危接口请求携带一次性 {@code X-Nonce}，服务端首次见到时占用，后续重复出现判定为重放。
 * 底层通常基于 Redis {@code SET key NX EX} 实现。</p>
 */
public interface NonceStore {

    /**
     * 尝试占用一个 nonce。
     *
     * @param nonce     一次性随机串
     * @param ttlMillis 占用有效期（毫秒），通常等于签名时间窗
     * @return true 表示首次占用成功（放行）；false 表示已存在（重放，应拦截）
     */
    boolean tryAcquire(String nonce, long ttlMillis);
}
