package com.ceams.common.sign;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 基于 Redis 的 nonce 防重放实现。
 *
 * <p>使用 {@code SET key NX EX}（即 {@link StringRedisTemplate#opsForValue()#setIfAbsent}） 原子占用，key 形如
 * {@code ceams:sign:nonce:{nonce}}，TTL 等于时间窗毫秒。 首次写入成功返回 true，已存在返回 false（重放拦截）。
 */
@Component
@RequiredArgsConstructor
public class RedisNonceStore implements NonceStore {

    private static final String KEY_PREFIX = "ceams:sign:nonce:";

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public boolean tryAcquire(String nonce, long ttlMillis) {
        String key = KEY_PREFIX + nonce;
        Boolean acquired =
                stringRedisTemplate
                        .opsForValue()
                        .setIfAbsent(key, "1", Duration.ofMillis(ttlMillis));
        return Boolean.TRUE.equals(acquired);
    }
}
