package com.ceams.common.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 轻量业务缓存组件。
 *
 * <p>不引入 Spring Cache / Redisson / Caffeine，直接基于 {@link StringRedisTemplate} + {@link
 * ObjectMapper} 手动读写 JSON。任何异常（Redis 不可用、序列化失败等）一律降级为调用方直查数据库， 仅记录 warn
 * 日志，不影响业务主流程。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BusinessCache {

    /** TTL 随机抖动上限（秒），避免缓存同时过期造成数据库瞬时压力。 */
    private static final int TTL_JITTER_MAX_SECONDS = 120;

    /** 命中指标名。 */
    private static final String METRIC_HIT = "ceams.cache.hit";

    /** 未命中指标名。 */
    private static final String METRIC_MISS = "ceams.cache.miss";

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final ObjectProvider<MeterRegistry> meterRegistryProvider;

    /** 业务缓存总开关，默认开启。 */
    @Value("${ceams.cache.enabled:true}")
    private boolean enabled;

    /**
     * 读取缓存。
     *
     * @param <T> 返回值类型
     * @param key 缓存 key
     * @param type 反序列化目标类型（含泛型）
     * @return 缓存值；未命中、缓存关闭或读取/反序列化失败时返回 {@code null}（由调用方直查数据库）
     */
    public <T> T get(String key, TypeReference<T> type) {
        if (!enabled) {
            return null;
        }
        try {
            String json = stringRedisTemplate.opsForValue().get(key);
            if (!StringUtils.hasText(json)) {
                count(METRIC_MISS, key);
                return null;
            }
            T value = objectMapper.readValue(json, type);
            count(METRIC_HIT, key);
            return value;
        } catch (Exception e) {
            log.warn("读取业务缓存失败，已降级直查数据库，key={}", key, e);
            return null;
        }
    }

    /**
     * 写入缓存，实际 TTL 在给定值基础上叠加 0～120 秒随机抖动。
     *
     * @param key 缓存 key
     * @param value 待缓存对象（为 {@code null} 时跳过写入）
     * @param ttl 基础有效期
     */
    public void put(String key, Object value, Duration ttl) {
        if (!enabled || key == null || value == null || ttl == null) {
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(value);
            long jitter = ThreadLocalRandom.current().nextLong(TTL_JITTER_MAX_SECONDS + 1L);
            long seconds = ttl.getSeconds() + jitter;
            stringRedisTemplate.opsForValue().set(key, json, Duration.ofSeconds(seconds));
        } catch (Exception e) {
            log.warn("写入业务缓存失败，已忽略本次缓存，key={}", key, e);
        }
    }

    /**
     * 删除单个缓存 key。
     *
     * @param key 缓存 key
     */
    public void evict(String key) {
        if (!enabled || key == null) {
            return;
        }
        try {
            stringRedisTemplate.delete(key);
        } catch (Exception e) {
            log.warn("删除业务缓存失败，key={}", key, e);
        }
    }

    /**
     * 按前缀批量删除缓存。
     *
     * <p>内部使用 {@code KEYS} 扫描，仅适用于低基数的字典类缓存（如分类树按类型缓存）。 禁止对业务大数据使用，避免阻塞
     * Redis 主线程。
     *
     * @param prefix key 前缀
     */
    public void evictByPrefix(String prefix) {
        if (!enabled || !StringUtils.hasText(prefix)) {
            return;
        }
        try {
            Set<String> keys = stringRedisTemplate.keys(prefix + "*");
            if (keys != null && !keys.isEmpty()) {
                stringRedisTemplate.delete(keys);
            }
        } catch (Exception e) {
            log.warn("按前缀删除业务缓存失败，prefix={}", prefix, e);
        }
    }

    /**
     * 业务缓存是否启用（配置项 {@code ceams.cache.enabled}，默认 true）。
     *
     * @return 开启返回 true
     */
    public boolean isEnabled() {
        return enabled;
    }

    /** 记录命中/未命中指标；未装配 MeterRegistry 时静默跳过。 */
    private void count(String metric, String key) {
        MeterRegistry registry = meterRegistryProvider.getIfAvailable();
        if (registry == null) {
            return;
        }
        registry.counter(metric, "cache", resolveCacheTag(key)).increment();
    }

    /** 从 key 中提取业务域作为指标 tag，如 {@code iam}、{@code masterdata}。 */
    private String resolveCacheTag(String key) {
        if (key == null) {
            return "unknown";
        }
        String body =
                key.startsWith(CacheKeys.PREFIX)
                        ? key.substring(CacheKeys.PREFIX.length())
                        : key;
        int index = body.indexOf(':');
        return index > 0 ? body.substring(0, index) : body;
    }
}
