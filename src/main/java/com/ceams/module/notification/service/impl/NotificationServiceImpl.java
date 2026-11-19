package com.ceams.module.notification.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.common.response.PageResult;
import com.ceams.module.iam.entity.SysRole;
import com.ceams.module.iam.entity.SysUser;
import com.ceams.module.iam.entity.UserRole;
import com.ceams.module.iam.enums.RoleStatus;
import com.ceams.module.iam.enums.UserStatus;
import com.ceams.module.iam.mapper.RoleMapper;
import com.ceams.module.iam.mapper.UserMapper;
import com.ceams.module.iam.mapper.UserRoleMapper;
import com.ceams.module.notification.converter.NotificationConverter;
import com.ceams.module.notification.dto.NotificationCreateRequest;
import com.ceams.module.notification.dto.NotificationPublishRequest;
import com.ceams.module.notification.dto.NotificationPublishScope;
import com.ceams.module.notification.dto.NotificationRecipientRoleVO;
import com.ceams.module.notification.dto.NotificationRecipientUserVO;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** 通知服务实现。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    /** 发布范围选人接口单次返回上限。 */
    private static final int RECIPIENT_USER_LIMIT = 50;

    private final NotificationMapper notificationMapper;
    private final NotificationConverter notificationConverter;
    private final UserNotificationPreferenceMapper userNotificationPreferenceMapper;
    private final UserNotificationMapper userNotificationMapper;
    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;

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

        for (Long userId : distinctIds(targetUserIds)) {
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

    /** 目标 ID 去重并忽略空值（保持原有顺序）。 */
    private List<Long> distinctIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        Set<Long> distinct = new LinkedHashSet<>();
        for (Long id : ids) {
            if (id != null) {
                distinct.add(id);
            }
        }
        return new ArrayList<>(distinct);
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
        publish(id, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int publish(Long id, NotificationPublishRequest request) {
        Notification notification = requireNotification(id);
        if (notification.getStatus() != NotificationStatus.DRAFT) {
            throw BusinessException.of(ErrorCode.BIZ_STATE_CONFLICT, "仅草稿状态可发布");
        }
        LocalDateTime now = LocalDateTime.now();
        notification.setStatus(NotificationStatus.PUBLISHED);
        notification.setPublishTime(now);
        notificationMapper.updateById(notification);

        if (request == null) {
            // 兼容旧调用：未传接收范围时沿用按用户偏好分渠道分发
            return distribute(notification);
        }
        return publishToScope(notification, request, now);
    }

    @Override
    public List<NotificationRecipientRoleVO> listRecipientRoles() {
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRole::getStatus, RoleStatus.ACTIVE);
        wrapper.orderByAsc(SysRole::getName);
        return roleMapper.selectList(wrapper).stream()
                .map(
                        role -> {
                            NotificationRecipientRoleVO vo = new NotificationRecipientRoleVO();
                            vo.setId(role.getId());
                            vo.setName(role.getName());
                            vo.setCode(role.getCode());
                            return vo;
                        })
                .toList();
    }

    @Override
    public List<NotificationRecipientUserVO> searchRecipientUsers(String keyword) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getStatus, UserStatus.ACTIVE);
        if (StringUtils.hasText(keyword)) {
            String trimmed = keyword.trim();
            wrapper.and(
                    w ->
                            w.like(SysUser::getName, trimmed)
                                    .or()
                                    .like(SysUser::getUsername, trimmed));
        }
        wrapper.orderByAsc(SysUser::getName);
        // 单次最多返回 50 条，避免选人下拉数据量过大
        IPage<SysUser> result =
                userMapper.selectPage(new Page<>(1, RECIPIENT_USER_LIMIT), wrapper);
        return result.getRecords().stream()
                .map(
                        user -> {
                            NotificationRecipientUserVO vo = new NotificationRecipientUserVO();
                            vo.setId(user.getId());
                            vo.setName(user.getName());
                            vo.setUsername(user.getUsername());
                            return vo;
                        })
                .toList();
    }

    /**
     * 按管理员指定范围直接生成系统站内信。
     *
     * <p>管理员显式指定范围，不再受用户偏好（含免打扰）限制，统一写入 SYSTEM、push_status=SENT
     * 的站内信；本阶段不生成邮件/短信记录。
     *
     * @return 实际写入的站内信条数（去重后的有效用户数）
     */
    private int publishToScope(
            Notification notification, NotificationPublishRequest request, LocalDateTime now) {
        List<Long> userIds = resolveRecipientUserIds(request);
        for (Long userId : userIds) {
            insertUserNotification(
                    notification.getId(), userId, PushChannel.SYSTEM, PushStatus.SENT, now);
        }
        log.info(
                "通知 {} 按范围 {} 发布，实际送达 {} 人",
                notification.getId(),
                request.getScope(),
                userIds.size());
        return userIds.size();
    }

    /**
     * 解析发布接收范围，返回去重后的有效用户 ID。
     *
     * <p>用户查询统一过滤 status=ACTIVE；逻辑删除条件（deleted=0）由 MyBatis-Plus 自动追加。
     */
    private List<Long> resolveRecipientUserIds(NotificationPublishRequest request) {
        NotificationPublishScope scope = request.getScope();
        if (scope == null) {
            throw BusinessException.of(ErrorCode.VALID_REQUIRED_FIELD, "请选择接收范围");
        }
        return switch (scope) {
            case ALL -> selectActiveUserIds(null);
            case ROLES -> {
                List<Long> roleIds = distinctIds(request.getRoleIds());
                if (roleIds.isEmpty()) {
                    throw BusinessException.of(ErrorCode.VALID_REQUIRED_FIELD, "请选择接收角色");
                }
                List<Long> candidateUserIds =
                        userRoleMapper
                                .selectList(
                                        new LambdaQueryWrapper<UserRole>()
                                                .select(UserRole::getUserId)
                                                .in(UserRole::getRoleId, roleIds))
                                .stream()
                                .map(UserRole::getUserId)
                                .toList();
                yield selectActiveUserIds(candidateUserIds);
            }
            case USERS -> {
                List<Long> userIds = distinctIds(request.getUserIds());
                if (userIds.isEmpty()) {
                    throw BusinessException.of(ErrorCode.VALID_REQUIRED_FIELD, "请选择接收用户");
                }
                yield selectActiveUserIds(userIds);
            }
        };
    }

    /**
     * 查询启用用户 ID（去重）。
     *
     * @param candidateUserIds 候选人集合，为 {@code null} 表示全部用户；为空集合时直接返回空列表， 避免生成
     *     {@code IN ()} 非法 SQL
     */
    private List<Long> selectActiveUserIds(Collection<Long> candidateUserIds) {
        if (candidateUserIds != null && candidateUserIds.isEmpty()) {
            return List.of();
        }
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(SysUser::getId);
        wrapper.eq(SysUser::getStatus, UserStatus.ACTIVE);
        if (candidateUserIds != null) {
            wrapper.in(SysUser::getId, new LinkedHashSet<>(candidateUserIds));
        }
        return userMapper.selectList(wrapper).stream().map(SysUser::getId).toList();
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
     *
     * @return 实际写入的 SYSTEM 站内信条数（即实际送达人数，不含邮件/短信记录）
     */
    private int distribute(Notification notification) {
        LocalDateTime now = LocalDateTime.now();
        LambdaQueryWrapper<UserNotificationPreference> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(
                UserNotificationPreference::getNotificationType,
                notification.getNotificationType());
        List<UserNotificationPreference> preferences =
                userNotificationPreferenceMapper.selectList(wrapper);

        int delivered = 0;
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
                    delivered++;
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
        return delivered;
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
