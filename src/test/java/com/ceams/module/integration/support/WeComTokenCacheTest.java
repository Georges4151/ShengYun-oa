package com.ceams.module.integration.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ceams.module.integration.client.AccessToken;
import com.ceams.module.integration.client.WeComApiClient;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

/** 企业微信 token 缓存单元测试：缓存命中不调用接口、未命中刷新并写缓存。 */
@ExtendWith(MockitoExtension.class)
class WeComTokenCacheTest {

    @Mock private StringRedisTemplate stringRedisTemplate;

    @Mock private ValueOperations<String, String> valueOperations;

    @Mock private WeComApiClient weComApiClient;

    @InjectMocks private WeComTokenCache tokenCache;

    @Test
    void shouldReuseCachedToken() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(WeComTokenCache.TOKEN_KEY)).thenReturn("cached-token");

        assertThat(tokenCache.getToken()).isEqualTo("cached-token");

        verify(weComApiClient, never()).fetchAccessToken();
        verify(valueOperations, never()).set(anyString(), anyString(), any(Duration.class));
    }

    @Test
    void shouldFetchAndCacheTokenWhenMiss() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(WeComTokenCache.TOKEN_KEY)).thenReturn(null);
        when(valueOperations.setIfAbsent(
                        eq(WeComTokenCache.LOCK_KEY), anyString(), any(Duration.class)))
                .thenReturn(true);
        when(weComApiClient.fetchAccessToken()).thenReturn(new AccessToken("fresh-token", 7200));

        assertThat(tokenCache.getToken()).isEqualTo("fresh-token");

        verify(valueOperations)
                .set(
                        eq(WeComTokenCache.TOKEN_KEY),
                        eq("fresh-token"),
                        eq(Duration.ofSeconds(6900)));
        verify(stringRedisTemplate).delete(WeComTokenCache.LOCK_KEY);
    }
}
