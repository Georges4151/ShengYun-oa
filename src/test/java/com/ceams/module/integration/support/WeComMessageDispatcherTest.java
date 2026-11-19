package com.ceams.module.integration.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.ceams.module.integration.client.WeComApiClient;
import com.ceams.module.integration.client.WeComApiException;
import com.ceams.module.integration.config.WeComAppProperties;
import com.ceams.module.integration.entity.IntegrationWecomMessage;
import com.ceams.module.integration.mapper.IntegrationWecomMessageMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

/** 企业微信应用消息调度器单元测试：成功置 SENT、失败退避 1m、超限置 FAILED、token 失效重试一次。 */
@ExtendWith(MockitoExtension.class)
class WeComMessageDispatcherTest {

    /** 解析 UpdateWrapper.set 生成的 SET 片段：列名 -> 参数占位符。 */
    private static final Pattern SET_PATTERN =
            Pattern.compile("([a-z_]+)=#\\{ew\\.paramNameValuePairs\\.(\\w+)}");

    @Mock private IntegrationWecomMessageMapper messageMapper;

    @Mock private WeComApiClient weComApiClient;

    @Mock private WeComTokenCache tokenCache;

    @Mock private StringRedisTemplate stringRedisTemplate;

    @Mock private ValueOperations<String, String> valueOperations;

    private WeComMessageDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        WeComAppProperties properties = new WeComAppProperties();
        properties.setEnabled(true);
        properties.setCorpId("corp-id");
        properties.setSecret("corp-secret");
        properties.setMessageRetryMax(5);
        dispatcher =
                new WeComMessageDispatcher(
                        messageMapper,
                        weComApiClient,
                        tokenCache,
                        properties,
                        stringRedisTemplate,
                        new ObjectMapper());
    }

    @Test
    void shouldMarkSentWhenWeComAcceptsMessage() {
        stubLockAcquired();
        IntegrationWecomMessage message = dueMessage(900L, 0);
        when(messageMapper.selectList(any())).thenReturn(List.of(message));
        when(tokenCache.getToken()).thenReturn("tk");

        dispatcher.dispatch();

        Map<String, Object> sets = captureUpdateSets();
        assertThat(sets.get("status")).isEqualTo("SENT");
        assertThat(sets.get("sent_at")).isInstanceOf(LocalDateTime.class);
        assertThat(sets).containsKey("error_msg");
        assertThat(sets.get("error_msg")).isNull();
        verify(weComApiClient)
                .sendAppMessage(eq("tk"), eq("wx900"), eq("审批待办提醒\n您有一条新的审批待办"));
    }

    @Test
    void shouldBackoffOneMinuteOnFirstFailure() {
        stubLockAcquired();
        IntegrationWecomMessage message = dueMessage(901L, 0);
        when(messageMapper.selectList(any())).thenReturn(List.of(message));
        when(tokenCache.getToken()).thenReturn("tk");
        doThrow(new WeComApiException(93000, "invalid ip"))
                .when(weComApiClient)
                .sendAppMessage(anyString(), anyString(), anyString());
        LocalDateTime before = LocalDateTime.now();

        dispatcher.dispatch();

        Map<String, Object> sets = captureUpdateSets();
        assertThat(sets.get("status")).isEqualTo("PENDING");
        assertThat(sets.get("retry_count")).isEqualTo(1);
        assertThat(sets.get("error_msg")).isEqualTo("invalid ip");
        LocalDateTime nextRetryAt = (LocalDateTime) sets.get("next_retry_at");
        assertThat(nextRetryAt).isAfter(before.plusSeconds(55)).isBefore(before.plusSeconds(65));
    }

    @Test
    void shouldMarkFailedWhenRetryLimitReached() {
        stubLockAcquired();
        IntegrationWecomMessage message = dueMessage(902L, 4);
        when(messageMapper.selectList(any())).thenReturn(List.of(message));
        when(tokenCache.getToken()).thenReturn("tk");
        doThrow(new WeComApiException(93000, "user not found"))
                .when(weComApiClient)
                .sendAppMessage(anyString(), anyString(), anyString());

        dispatcher.dispatch();

        Map<String, Object> sets = captureUpdateSets();
        assertThat(sets.get("status")).isEqualTo("FAILED");
        assertThat(sets.get("retry_count")).isEqualTo(5);
        assertThat(sets).containsKey("next_retry_at");
        assertThat(sets.get("next_retry_at")).isNull();
    }

    @Test
    void shouldInvalidateTokenAndRetryOnceWhenTokenExpired() {
        stubLockAcquired();
        IntegrationWecomMessage message = dueMessage(903L, 0);
        when(messageMapper.selectList(any())).thenReturn(List.of(message));
        when(tokenCache.getToken()).thenReturn("tk-old", "tk-new");
        doThrow(new WeComApiException(40014, "invalid access_token"))
                .doNothing()
                .when(weComApiClient)
                .sendAppMessage(anyString(), anyString(), anyString());

        dispatcher.dispatch();

        verify(tokenCache).invalidate();
        verify(weComApiClient, times(2))
                .sendAppMessage(anyString(), anyString(), anyString());
        verify(weComApiClient).sendAppMessage(eq("tk-new"), eq("wx903"), anyString());
        assertThat(captureUpdateSets().get("status")).isEqualTo("SENT");
    }

    // ------------------------------------------------------------------
    // 测试工具
    // ------------------------------------------------------------------

    private void stubLockAcquired() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(
                        eq(WeComMessageDispatcher.DISPATCH_LOCK_KEY), anyString(), any(Duration.class)))
                .thenReturn(true);
    }

    private IntegrationWecomMessage dueMessage(Long id, int retryCount) {
        IntegrationWecomMessage message = new IntegrationWecomMessage();
        message.setId(id);
        message.setTargetUserId(id);
        message.setWecomUserid("wx" + id);
        message.setMsgType("TEXT");
        message.setStatus("PENDING");
        message.setRetryCount(retryCount);
        message.setNextRetryAt(LocalDateTime.now().minusSeconds(1));
        message.setPayloadJson(
                "{\"title\":\"审批待办提醒\",\"content\":\"您有一条新的审批待办\",\"linkUrl\":\"ceams://todo?tab=pending\"}");
        return message;
    }

    private Map<String, Object> captureUpdateSets() {
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Wrapper<IntegrationWecomMessage>> captor =
                ArgumentCaptor.forClass(Wrapper.class);
        verify(messageMapper).update(any(), captor.capture());
        @SuppressWarnings("unchecked")
        UpdateWrapper<IntegrationWecomMessage> wrapper =
                (UpdateWrapper<IntegrationWecomMessage>) captor.getValue();
        Map<String, Object> sets = new LinkedHashMap<>();
        Matcher matcher = SET_PATTERN.matcher(wrapper.getSqlSet());
        while (matcher.find()) {
            sets.put(matcher.group(1), wrapper.getParamNameValuePairs().get(matcher.group(2)));
        }
        return sets;
    }
}
