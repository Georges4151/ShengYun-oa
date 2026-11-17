package com.ceams.module.integration.support;

import com.ceams.module.integration.client.AccessToken;
import com.ceams.module.integration.client.WeComApiClient;
import com.ceams.module.integration.client.WeComApiException;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 企业微信 access_token 缓存。
 *
 * <p>Redis {@code wecom:token} 为唯一事实来源，TTL 取 {@code expires_in - 300} 秒（最小 60 秒），
 * 避免临界过期。缓存未命中时通过 {@code SET NX EX 10} 争抢 {@code wecom:token:lock}，
 * 抢锁成功后双重检查并刷新，抢锁失败则再读一次缓存，仍为空才抛错。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WeComTokenCache {

    /** token 缓存 key。 */
    static final String TOKEN_KEY = "wecom:token";

    /** 刷新锁 key。 */
    static final String LOCK_KEY = "wecom:token:lock";

    /** 刷新锁有效期（秒）。 */
    private static final Duration LOCK_TTL = Duration.ofSeconds(10);

    /** 提前过期秒数，避免临界失效。 */
    private static final int EXPIRE_SAFETY_SECONDS = 300;

    /** token 最小有效期（秒）。 */
    private static final int MIN_TTL_SECONDS = 60;

    private final StringRedisTemplate stringRedisTemplate;
    private final WeComApiClient weComApiClient;

    /**
     * 获取有效 access_token（优先读缓存）。
     *
     * @return 企业微信 access_token
     * @throws WeComApiException 抢锁失败且缓存仍为空，或企业微信接口调用失败
     */
    public String getToken() {
        String cached = stringRedisTemplate.opsForValue().get(TOKEN_KEY);
        if (StringUtils.hasText(cached)) {
            return cached;
        }
        Boolean acquired = stringRedisTemplate.opsForValue().setIfAbsent(LOCK_KEY, "1", LOCK_TTL);
        if (!Boolean.TRUE.equals(acquired)) {
            String retried = stringRedisTemplate.opsForValue().get(TOKEN_KEY);
            if (StringUtils.hasText(retried)) {
                return retried;
            }
            throw new WeComApiException(-1, "获取企业微信 access_token 超时，请稍后重试");
        }
        try {
            // 双重检查：抢锁期间可能已有其他线程完成刷新
            String doubleChecked = stringRedisTemplate.opsForValue().get(TOKEN_KEY);
            if (StringUtils.hasText(doubleChecked)) {
                return doubleChecked;
            }
            AccessToken accessToken = weComApiClient.fetchAccessToken();
            long ttl =
                    Math.max(
                            MIN_TTL_SECONDS,
                            (long) accessToken.expiresIn() - EXPIRE_SAFETY_SECONDS);
            stringRedisTemplate
                    .opsForValue()
                    .set(TOKEN_KEY, accessToken.token(), Duration.ofSeconds(ttl));
            log.info("企业微信 access_token 已刷新，TTL={}秒", ttl);
            return accessToken.token();
        } finally {
            stringRedisTemplate.delete(LOCK_KEY);
        }
    }

    /** 使当前 token 失效（P4 发送失败且判定为 token 失效时调用）。 */
    public void invalidate() {
        stringRedisTemplate.delete(TOKEN_KEY);
        log.info("企业微信 access_token 缓存已清除");
    }
}
