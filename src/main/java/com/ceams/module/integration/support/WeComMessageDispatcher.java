package com.ceams.module.integration.support;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.ceams.module.integration.client.WeComApiClient;
import com.ceams.module.integration.client.WeComApiException;
import com.ceams.module.integration.config.WeComAppProperties;
import com.ceams.module.integration.entity.IntegrationWecomMessage;
import com.ceams.module.integration.enums.WeComMessageStatus;
import com.ceams.module.integration.mapper.IntegrationWecomMessageMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 企业微信应用消息调度器（发件箱消费者）。
 *
 * <p>固定延迟轮询发件箱，把到期的 {@code PENDING} 消息投递到企业微信：
 *
 * <ul>
 *   <li>Redis {@code wecom:dispatch:lock}（{@code SET NX EX 60}）保证多实例下同一轮只有一个执行者；
 *   <li>每轮最多 50 条，按 id 升序，避免单轮耗时过长；
 *   <li>token 失效（{@code 40014 / 42001}）时清缓存并在同一轮重试一次；
 *   <li>失败按 {@code 1m / 5m / 15m / 1h / 6h} 退避，超过 {@code ceams.wecom.message-retry-max}
 *       置 {@code FAILED}（可经接口手工重投）；
 *   <li>单条异常不影响其他条目，整体异常只记 {@code warn}。
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WeComMessageDispatcher {

    /** 调度互斥锁 key。 */
    static final String DISPATCH_LOCK_KEY = "wecom:dispatch:lock";

    /** 调度互斥锁有效期（秒），需大于单轮最长耗时。 */
    private static final long DISPATCH_LOCK_SECONDS = 60L;

    /** 单轮最多处理条数。 */
    private static final int BATCH_SIZE = 50;

    /** 消息正文最大长度（企业微信文本消息限制内，超长截断）。 */
    private static final int TEXT_MAX_LENGTH = 512;

    /** 失败原因最大长度（与表字段一致）。 */
    private static final int ERROR_MSG_MAX_LENGTH = 500;

    /** 判定 access_token 失效 / 过期的错误码。 */
    private static final Set<Integer> TOKEN_INVALID_ERRCODES = Set.of(40014, 42001);

    /** 失败退避秒数：1m / 5m / 15m / 1h / 6h（按新 retryCount 取档，超出取最后一档）。 */
    private static final long[] RETRY_BACKOFF_SECONDS = {60L, 300L, 900L, 3600L, 21600L};

    private final IntegrationWecomMessageMapper messageMapper;
    private final WeComApiClient weComApiClient;
    private final WeComTokenCache tokenCache;
    private final WeComAppProperties properties;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    /** 定时消费发件箱：未启用直接跳过，异常全吞仅记日志。 */
    @Scheduled(fixedDelayString = "${ceams.wecom.dispatch-interval-ms:30000}")
    public void dispatch() {
        if (!properties.isConfigured()) {
            log.debug("企业微信应用集成未启用，跳过应用消息调度");
            return;
        }
        try {
            String lockToken = UUID.randomUUID().toString();
            Boolean acquired =
                    stringRedisTemplate
                            .opsForValue()
                            .setIfAbsent(
                                    DISPATCH_LOCK_KEY,
                                    lockToken,
                                    Duration.ofSeconds(DISPATCH_LOCK_SECONDS));
            if (!Boolean.TRUE.equals(acquired)) {
                log.debug("企业微信应用消息调度锁被其他实例持有，本轮跳过");
                return;
            }
            try {
                deliverDueMessages();
            } finally {
                releaseLock(lockToken);
            }
        } catch (Exception e) {
            log.warn("企业微信应用消息调度失败（已忽略）：{}", e.getMessage(), e);
        }
    }

    // ------------------------------------------------------------------
    // 内部流程
    // ------------------------------------------------------------------

    /** 取出到期消息逐条投递，单条失败不影响其他条目。 */
    private void deliverDueMessages() {
        List<IntegrationWecomMessage> dueMessages = selectDueMessages();
        if (dueMessages.isEmpty()) {
            return;
        }
        for (IntegrationWecomMessage message : dueMessages) {
            try {
                deliver(message);
            } catch (Exception e) {
                log.warn(
                        "企业微信应用消息投递异常（已忽略单条）：messageId={}，原因={}",
                        message.getId(),
                        e.getMessage(),
                        e);
            }
        }
    }

    /** 到期消息：PENDING 且 next_retry_at 为空或已到期，按 id 升序最多 50 条。 */
    private List<IntegrationWecomMessage> selectDueMessages() {
        LocalDateTime now = LocalDateTime.now();
        QueryWrapper<IntegrationWecomMessage> wrapper =
                new QueryWrapper<IntegrationWecomMessage>()
                        .eq("status", WeComMessageStatus.PENDING.name())
                        .and(
                                condition ->
                                        condition
                                                .isNull("next_retry_at")
                                                .or()
                                                .le("next_retry_at", now))
                        .orderByAsc("id")
                        .last("LIMIT " + BATCH_SIZE);
        List<IntegrationWecomMessage> messages = messageMapper.selectList(wrapper);
        return messages == null ? List.of() : messages;
    }

    /** 投递单条消息：token 失效时清缓存并同轮重试一次。 */
    private void deliver(IntegrationWecomMessage message) {
        if (message == null || message.getId() == null || !StringUtils.hasText(message.getWecomUserid())) {
            return;
        }
        String text = buildText(message);
        try {
            String token = tokenCache.getToken();
            weComApiClient.sendAppMessage(token, message.getWecomUserid(), text);
            markSent(message.getId());
        } catch (WeComApiException e) {
            if (!isTokenInvalid(e)) {
                markFailed(message, e);
                return;
            }
            log.info("企业微信 access_token 失效，清理缓存后重试一次：messageId={}", message.getId());
            tokenCache.invalidate();
            try {
                String refreshedToken = tokenCache.getToken();
                weComApiClient.sendAppMessage(refreshedToken, message.getWecomUserid(), text);
                markSent(message.getId());
            } catch (Exception retryError) {
                markFailed(message, retryError);
            }
        } catch (Exception e) {
            markFailed(message, e);
        }
    }

    /** 投递成功：置 SENT、写发送时间并清空失败原因。 */
    private void markSent(Long messageId) {
        UpdateWrapper<IntegrationWecomMessage> wrapper =
                new UpdateWrapper<IntegrationWecomMessage>()
                        .eq("id", messageId)
                        .set("status", WeComMessageStatus.SENT.name())
                        .set("sent_at", LocalDateTime.now())
                        .set("error_msg", null);
        messageMapper.update(null, wrapper);
    }

    /** 投递失败：累加重试次数，达到上限置 FAILED，否则按退避时间回到 PENDING。 */
    private void markFailed(IntegrationWecomMessage message, Exception cause) {
        int retryCount = (message.getRetryCount() == null ? 0 : message.getRetryCount()) + 1;
        int retryMax = Math.max(1, properties.getMessageRetryMax());
        boolean giveUp = retryCount >= retryMax;
        UpdateWrapper<IntegrationWecomMessage> wrapper =
                new UpdateWrapper<IntegrationWecomMessage>()
                        .eq("id", message.getId())
                        .set("retry_count", retryCount)
                        .set("error_msg", truncate(errorText(cause), ERROR_MSG_MAX_LENGTH))
                        .set(
                                "next_retry_at",
                                giveUp ? null : LocalDateTime.now().plusSeconds(backoffSeconds(retryCount)))
                        .set(
                                "status",
                                giveUp
                                        ? WeComMessageStatus.FAILED.name()
                                        : WeComMessageStatus.PENDING.name());
        try {
            messageMapper.update(null, wrapper);
        } catch (Exception e) {
            log.warn("回写企业微信应用消息失败状态异常：messageId={}，原因={}", message.getId(), e.getMessage());
        }
        if (giveUp) {
            log.warn(
                    "企业微信应用消息重试已达上限，转 FAILED：messageId={}，retryCount={}，原因={}",
                    message.getId(),
                    retryCount,
                    errorText(cause));
        } else {
            log.info(
                    "企业微信应用消息投递失败，{}秒后重试：messageId={}，retryCount={}，原因={}",
                    backoffSeconds(retryCount),
                    message.getId(),
                    retryCount,
                    errorText(cause));
        }
    }

    /** 按新 retryCount 取退避档位（1m / 5m / 15m / 1h / 6h，超出取最后一档）。 */
    private long backoffSeconds(int retryCount) {
        int index = Math.min(Math.max(retryCount, 1), RETRY_BACKOFF_SECONDS.length) - 1;
        return RETRY_BACKOFF_SECONDS[index];
    }

    /** 组装正文：标题 + 换行 + 正文，http/https 链接追加「详情」，整体截断 512 字符。 */
    private String buildText(IntegrationWecomMessage message) {
        String title = null;
        String content = null;
        String linkUrl = null;
        String payloadJson = message.getPayloadJson();
        if (StringUtils.hasText(payloadJson)) {
            try {
                JsonNode payload = objectMapper.readTree(payloadJson);
                title = readText(payload, "title");
                content = readText(payload, "content");
                linkUrl = readText(payload, "linkUrl");
            } catch (Exception e) {
                log.warn(
                        "企业微信应用消息正文解析失败（降级为空正文）：messageId={}，原因={}",
                        message.getId(),
                        e.getMessage());
            }
        }
        StringBuilder text = new StringBuilder();
        text.append(title == null ? "" : title).append('\n').append(content == null ? "" : content);
        if (StringUtils.hasText(linkUrl)
                && (linkUrl.startsWith("http://") || linkUrl.startsWith("https://"))) {
            text.append('\n').append("详情：").append(linkUrl);
        }
        return truncate(text.toString(), TEXT_MAX_LENGTH);
    }

    private String readText(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private boolean isTokenInvalid(WeComApiException e) {
        return e != null && TOKEN_INVALID_ERRCODES.contains(e.getErrcode());
    }

    private String errorText(Exception cause) {
        if (cause == null) {
            return "未知错误";
        }
        if (cause instanceof WeComApiException weComApiException) {
            String errmsg = weComApiException.getErrmsg();
            return StringUtils.hasText(errmsg)
                    ? errmsg
                    : "企业微信接口返回 errcode=" + weComApiException.getErrcode();
        }
        return StringUtils.hasText(cause.getMessage()) ? cause.getMessage() : cause.getClass().getSimpleName();
    }

    private String truncate(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private void releaseLock(String lockToken) {
        try {
            String holder = stringRedisTemplate.opsForValue().get(DISPATCH_LOCK_KEY);
            if (lockToken.equals(holder)) {
                stringRedisTemplate.delete(DISPATCH_LOCK_KEY);
            }
        } catch (Exception e) {
            log.warn("释放企业微信应用消息调度锁异常：{}", e.getMessage());
        }
    }
}
