package com.ceams.module.notification.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.common.response.PageResult;
import com.ceams.module.notification.converter.NotificationConverter;
import com.ceams.module.notification.dto.NotificationCreateRequest;
import com.ceams.module.notification.dto.NotificationUpdateRequest;
import com.ceams.module.notification.entity.Notification;
import com.ceams.module.notification.entity.UserNotification;
import com.ceams.module.notification.entity.UserNotificationPreference;
import com.ceams.module.notification.enums.NotificationPriority;
import com.ceams.module.notification.enums.NotificationStatus;
import com.ceams.module.notification.enums.NotificationType;
import com.ceams.module.notification.enums.PushChannel;
import com.ceams.module.notification.enums.PushStatus;
import com.ceams.module.notification.mapper.NotificationMapper;
import com.ceams.module.notification.mapper.UserNotificationMapper;
import com.ceams.module.notification.mapper.UserNotificationPreferenceMapper;
import com.ceams.module.notification.service.NotificationService;
import com.ceams.module.notification.util.PageUtil;
import com.ceams.module.notification.vo.NotificationVO;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 通知服务实现。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationMapper notificationMapper;
    private final NotificationConverter notificationConverter;
    private final UserNotificationPreferenceMapper userNotificationPreferenceMapper;
    private final UserNotificationMapper userNotificationMapper;

    @Override
    public PageResult<NotificationVO> page(
            long page,
            long size,
            NotificationType type,
            NotificationStatus status,
            NotificationPriority priority,
            LocalDateTime startTime,
            LocalDateTime endTime) {
        Page<Notification> p = PageUtil.resolve(page, size);
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(type != null, Notification::getNotificationType, type);
        wrapper.eq(status != null, Notification::getStatus, status);
        wrapper.eq(priority != null, Notification::getPriority, priority);
        if (startTime != null) {
            wrapper.ge(Notification::getValidStartTime, startTime);
        }
        if (endTime != null) {
            wrapper.le(Notification::getValidEndTime, endTime);
        }
        wrapper.orderByDesc(Notification::getCreateTime);
        IPage<Notification> result = notificationMapper.selectPage(p, wrapper);
        return new PageResult<>(
                notificationConverter.toVOList(result.getRecords()),
                result.getTotal(),
                result.getCurrent(),
                result.getSize());
    }

    @Override
    public NotificationVO detail(Long id) {
        return notificationConverter.toVO(requireNotification(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String create(NotificationCreateRequest request) {
        Notification notification = fromRequest(request);
        notification.setStatus(NotificationStatus.DRAFT);
        notificationMapper.insert(notification);
        return String.valueOf(notification.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String createAndPublishToUsers(
            NotificationCreateRequest request, Collection<Long> targetUserIds) {
        LocalDateTime now = LocalDateTime.now();
        Notification notification = fromRequest(request);
        notification.setStatus(NotificationStatus.PUBLISHED);
        notification.setPublishTime(now);
        notificationMapper.insert(notification);

        for (Long userId : distinctUserIds(targetUserIds)) {
            if (existsSystemNotification(notification.getId(), userId)) {
                log.info("通知 {} 已定向投递过用户 {}，跳过重复写入", notification.getId(), userId);
                continue;
            }
            insertUserNotification(
                    notification.getId(), userId, PushChannel.SYSTEM, PushStatus.SENT, now);
        }
        return String.valueOf(notification.getId());
    }

    @Override
    public boolean existsRecent(NotificationType type, String linkUrl, LocalDateTime since) {
        if (type == null || linkUrl == null) {
            return false;
        }
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getNotificationType, type)
                .eq(Notification::getLinkUrl, linkUrl);
        if (since != null) {
            wrapper.ge(Notification::getCreateTime, since);
        }
        Long count = notificationMapper.selectCount(wrapper);
        return count != null && count > 0;
    }

    /** 按请求组装通知字段（不含状态）。 */
    private Notification fromRequest(NotificationCreateRequest request) {
        Notification notification = new Notification();
        notification.setTitle(request.getTitle());
        notification.setContentType(request.getContentType());
        notification.setContent(request.getContent());
        notification.setLinkUrl(request.getLinkUrl());
        notification.setPriority(
                request.getPriority() == null
                        ? NotificationPriority.NORMAL
                        : request.getPriority());
        notification.setNotificationType(request.getNotificationType());
        notification.setSenderId(request.getSenderId());
        notification.setSenderName(request.getSenderName());
        notification.setValidStartTime(request.getValidStartTime());
        notification.setValidEndTime(request.getValidEndTime());
        return notification;
    }

    /** 目标用户去重并忽略空值。 */
    private List<Long> distinctUserIds(Collection<Long> targetUserIds) {
        if (targetUserIds == null || targetUserIds.isEmpty()) {
            return List.of();
        }
        return targetUserIds.stream().filter(Objects::nonNull).distinct().toList();
    }

    /** 同一通知 + 用户 + SYSTEM 渠道是否已存在（幂等判断）。 */
    private boolean existsSystemNotification(Long notificationId, Long userId) {
        LambdaQueryWrapper<UserNotification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserNotification::getNotificationId, notificationId)
                .eq(UserNotification::getUserId, userId)
                .eq(UserNotification::getChannel, PushChannel.SYSTEM);
        Long count = userNotificationMapper.selectCount(wrapper);
        return count != null && count > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, NotificationUpdateRequest request) {
        Notification notification = requireNotification(id);
        if (notification.getStatus() != NotificationStatus.DRAFT) {
            throw BusinessException.of(ErrorCode.BIZ_STATE_CONFLICT, "仅草稿状态可编辑");
        }
        if (request.getTitle() != null) {
            notification.setTitle(request.getTitle());
        }
        if (request.getContentType() != null) {
            notification.setContentType(request.getContentType());
        }
        if (request.getContent() != null) {
            notification.setContent(request.getContent());
        }
        if (request.getLinkUrl() != null) {
            notification.setLinkUrl(request.getLinkUrl());
        }
        if (request.getPriority() != null) {
            notification.setPriority(request.getPriority());
        }
        if (request.getNotificationType() != null) {
            notification.setNotificationType(request.getNotificationType());
        }
        if (request.getSenderId() != null) {
            notification.setSenderId(request.getSenderId());
        }
        if (request.getSenderName() != null) {
            notification.setSenderName(request.getSenderName());
        }
        if (request.getValidStartTime() != null) {
            notification.setValidStartTime(request.getValidStartTime());
        }
        if (request.getValidEndTime() != null) {
            notification.setValidEndTime(request.getValidEndTime());
        }
        notificationMapper.updateById(notification);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Notification notification = requireNotification(id);
        if (notification.getStatus() != NotificationStatus.DRAFT) {
            throw BusinessException.of(ErrorCode.BIZ_STATE_CONFLICT, "仅草稿状态可删除");
        }
        notificationMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publish(Long id) {
        Notification notification = requireNotification(id);
        if (notification.getStatus() != NotificationStatus.DRAFT) {
            throw BusinessException.of(ErrorCode.BIZ_STATE_CONFLICT, "仅草稿状态可发布");
        }
        notification.setStatus(NotificationStatus.PUBLISHED);
        notification.setPublishTime(LocalDateTime.now());
        notificationMapper.updateById(notification);
        distribute(notification);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revoke(Long id) {
        Notification notification = requireNotification(id);
        if (notification.getStatus() != NotificationStatus.PUBLISHED) {
            throw BusinessException.of(ErrorCode.BIZ_STATE_CONFLICT, "仅已发布状态可撤销");
        }
        notification.setStatus(NotificationStatus.REVOKED);
        notificationMapper.updateById(notification);
        // 撤销后不再新增分发；已落库的 UserNotification 保留，由前端按通知状态提示失效。
    }

    /**
     * 发布后按偏好分渠道分发。
     *
     * <ul>
     *   <li>receive_system=1 且未命中免打扰 -> 生成 SYSTEM 站内信（push_status=SENT）。
     *   <li>receive_email=1 -> 生成 EMAIL（push_status=PENDING，本阶段不接真实外部通道，仅落库）。
     *   <li>receive_sms=1 -> 生成 SMS（push_status=PENDING，本阶段不接真实外部通道，仅落库）。
     *   <li>命中免打扰（quiet_mode=1 且当前时间在窗口内且 priority 不高于阈值）时，跳过 SYSTEM 站内信。
     * </ul>
     */
    private void distribute(Notification notification) {
        LocalDateTime now = LocalDateTime.now();
        LambdaQueryWrapper<UserNotificationPreference> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(
                UserNotificationPreference::getNotificationType,
                notification.getNotificationType());
        List<UserNotificationPreference> preferences =
                userNotificationPreferenceMapper.selectList(wrapper);

        for (UserNotificationPreference preference : preferences) {
            if (isEnabled(preference.getReceiveSystem())) {
                if (isQuiet(preference, notification.getPriority())) {
                    log.info(
                            "通知 {} 对用户 {} 命中免打扰，跳过 SYSTEM 站内信",
                            notification.getId(),
                            preference.getUserId());
                } else {
                    insertUserNotification(
                            notification.getId(),
                            preference.getUserId(),
                            PushChannel.SYSTEM,
                            PushStatus.SENT,
                            now);
                }
            }
            if (isEnabled(preference.getReceiveEmail())) {
                insertUserNotification(
                        notification.getId(),
                        preference.getUserId(),
                        PushChannel.EMAIL,
                        PushStatus.PENDING,
                        null);
            }
            if (isEnabled(preference.getReceiveSms())) {
                insertUserNotification(
                        notification.getId(),
                        preference.getUserId(),
                        PushChannel.SMS,
                        PushStatus.PENDING,
                        null);
            }
        }
    }

    private void insertUserNotification(
            Long notificationId,
            Long userId,
            PushChannel channel,
            PushStatus pushStatus,
            LocalDateTime pushTime) {
        UserNotification userNotification = new UserNotification();
        userNotification.setNotificationId(notificationId);
        userNotification.setUserId(userId);
        userNotification.setIsRead(0);
        userNotification.setChannel(channel);
        userNotification.setPushStatus(pushStatus);
        userNotification.setPushTime(pushTime);
        userNotificationMapper.insert(userNotification);
    }

    private boolean isEnabled(Integer flag) {
        return flag != null && flag == 1;
    }

    /**
     * 是否命中免打扰：quiet_mode=1 且当前时间落在 [quiet_start_time, quiet_end_time] 且 通知 priority 不高于
     * priority_threshold。
     *
     * <p>支持跨天窗口（start > end）。threshold 为 null 时视为开启免打扰即屏蔽（最保守）。
     */
    private boolean isQuiet(UserNotificationPreference preference, NotificationPriority priority) {
        if (!isEnabled(preference.getQuietMode())) {
            return false;
        }
        LocalTime start = preference.getQuietStartTime();
        LocalTime end = preference.getQuietEndTime();
        if (start == null || end == null) {
            return false;
        }
        LocalTime now = LocalTime.now();
        boolean inWindow;
        if (!start.isAfter(end)) {
            inWindow = !now.isBefore(start) && !now.isAfter(end);
        } else {
            inWindow = !now.isBefore(start) || !now.isAfter(end);
        }
        if (!inWindow) {
            return false;
        }
        NotificationPriority threshold = preference.getPriorityThreshold();
        if (threshold == null) {
            return true;
        }
        return priority.getLevel() <= threshold.getLevel();
    }

    private Notification requireNotification(Long id) {
        Notification notification = notificationMapper.selectById(id);
        if (notification == null) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "通知不存在");
        }
        return notification;
    }
}
