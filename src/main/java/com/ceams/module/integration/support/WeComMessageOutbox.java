package com.ceams.module.integration.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ceams.module.iam.entity.SysUser;
import com.ceams.module.iam.mapper.UserMapper;
import com.ceams.module.integration.config.WeComAppProperties;
import com.ceams.module.integration.entity.IntegrationWecomMessage;
import com.ceams.module.integration.enums.WeComMessageStatus;
import com.ceams.module.integration.mapper.IntegrationWecomMessageMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 企业微信应用消息发件箱（入队器）。
 *
 * <p>站内通知写成功后调用 {@link #enqueue}，为每个已绑定企业微信的目标用户写一行
 * {@code integration_wecom_message}（{@code status=PENDING}），由
 * {@link WeComMessageDispatcher} 异步投递。
 *
 * <p>可靠性口径：
 *
 * <ul>
 *   <li>未启用企业微信应用集成（{@code !properties.isConfigured()}）直接短路，不写任何记录；
 *   <li>{@code notification_id + target_user_id} 唯一键冲突视为「已入队」，静默跳过（幂等）；
 *   <li><b>任何异常只记录 {@code warn}，绝不向调用方抛出</b>：调用方是审批主流程，
 *       企业微信消息属可选增强，不得反向影响站内通知与审批事务。
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WeComMessageOutbox {

    /** 消息类型：当前仅文本。 */
    private static final String MSG_TYPE_TEXT = "TEXT";

    /** 消息体快照字段：标题。 */
    private static final String PAYLOAD_TITLE = "title";

    /** 消息体快照字段：正文。 */
    private static final String PAYLOAD_CONTENT = "content";

    /** 消息体快照字段：跳转链接。 */
    private static final String PAYLOAD_LINK_URL = "linkUrl";

    private final IntegrationWecomMessageMapper messageMapper;
    private final UserMapper userMapper;
    private final WeComAppProperties properties;
    private final ObjectMapper objectMapper;

    /**
     * 入队一批应用消息（每个已绑定企业微信的目标用户一行）。
     *
     * @param notificationId 站内通知 ID（雪花 ID 字符串）
     * @param targetUserIds 目标用户 ID 集合（可含空值与重复）
     * @param title 消息标题
     * @param content 消息正文
     * @param linkUrl 跳转链接（可为空）
     */
    public void enqueue(
            String notificationId,
            Collection<Long> targetUserIds,
            String title,
            String content,
            String linkUrl) {
        if (!properties.isConfigured()) {
            return;
        }
        if (!StringUtils.hasText(notificationId) || targetUserIds == null || targetUserIds.isEmpty()) {
            return;
        }
        List<Long> targets =
                targetUserIds.stream().filter(Objects::nonNull).distinct().toList();
        if (targets.isEmpty()) {
            return;
        }
        Long notificationPk = parseNotificationId(notificationId);
        if (notificationPk == null) {
            log.warn("企业微信应用消息入队跳过：站内通知 ID 非法（{}）", notificationId);
            return;
        }
        try {
            Map<Long, String> wecomUserIds = loadWeComUserIds(targets);
            if (wecomUserIds.isEmpty()) {
                log.debug("企业微信应用消息入队跳过：目标用户均未绑定企业微信，notificationId={}", notificationPk);
                return;
            }
            String payloadJson = toPayloadJson(title, content, linkUrl);
            LocalDateTime nextRetryAt = LocalDateTime.now();
            for (Map.Entry<Long, String> entry : wecomUserIds.entrySet()) {
                insertQuietly(notificationPk, entry.getKey(), entry.getValue(), payloadJson, nextRetryAt);
            }
        } catch (Exception e) {
            // 发件箱只是可选增强：任何异常都不得影响站内通知与审批事务
            log.warn(
                    "企业微信应用消息入队失败（已忽略，不影响通知与审批主流程）：notificationId={}，targets={}，原因={}",
                    notificationId,
                    targets,
                    e.getMessage(),
                    e);
        }
    }

    // ------------------------------------------------------------------
    // 内部工具
    // ------------------------------------------------------------------

    /** 查询目标用户中已绑定企业微信的成员账号（未删除用户由逻辑删除自动过滤）。 */
    private Map<Long, String> loadWeComUserIds(List<Long> targets) {
        List<SysUser> users =
                userMapper.selectList(
                        new LambdaQueryWrapper<SysUser>()
                                .in(SysUser::getId, targets)
                                .isNotNull(SysUser::getWecomUserid)
                                .ne(SysUser::getWecomUserid, ""));
        Map<Long, String> result = new LinkedHashMap<>();
        if (users == null) {
            return result;
        }
        for (SysUser user : users) {
            if (user == null || user.getId() == null || !StringUtils.hasText(user.getWecomUserid())) {
                continue;
            }
            result.put(user.getId(), user.getWecomUserid().trim());
        }
        return result;
    }

    /** 单条入队；唯一键冲突（重复入队）静默跳过。 */
    private void insertQuietly(
            Long notificationId,
            Long targetUserId,
            String wecomUserid,
            String payloadJson,
            LocalDateTime nextRetryAt) {
        IntegrationWecomMessage message = new IntegrationWecomMessage();
        message.setNotificationId(notificationId);
        message.setTargetUserId(targetUserId);
        message.setWecomUserid(wecomUserid);
        message.setMsgType(MSG_TYPE_TEXT);
        message.setPayloadJson(payloadJson);
        message.setStatus(WeComMessageStatus.PENDING.name());
        message.setRetryCount(0);
        message.setNextRetryAt(nextRetryAt);
        try {
            messageMapper.insert(message);
        } catch (DuplicateKeyException e) {
            log.debug(
                    "企业微信应用消息已入队，跳过重复写入：notificationId={}，targetUserId={}",
                    notificationId,
                    targetUserId);
        }
    }

    /** 消息体快照序列化（title/content/linkUrl）。 */
    private String toPayloadJson(String title, String content, String linkUrl) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put(PAYLOAD_TITLE, title);
        payload.put(PAYLOAD_CONTENT, content);
        payload.put(PAYLOAD_LINK_URL, linkUrl);
        return objectMapper.writeValueAsString(payload);
    }

    /** 站内通知 ID 由雪花 ID 字符串转 Long，解析失败返回 null。 */
    private Long parseNotificationId(String notificationId) {
        try {
            return Long.parseLong(notificationId.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
