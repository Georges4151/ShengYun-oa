package com.ceams.service;

import com.ceams.dao.NotificationMapper;
import com.ceams.dao.UserNotificationMapper;
import com.ceams.dao.UserNotificationPreferenceMapper;
import com.ceams.dao.UserMapper;
import com.ceams.model.entity.Notification;
import com.ceams.model.entity.User;
import com.ceams.model.entity.UserNotification;
import com.ceams.model.entity.UserNotificationPreference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    @Autowired
    private NotificationMapper notificationMapper;

    @Autowired
    private UserNotificationMapper userNotificationMapper;

    @Autowired
    private UserNotificationPreferenceMapper preferenceMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private NotificationChannelService channelService;

    @Transactional
    public Notification createNotification(Notification notification, Integer senderId) {
        User sender = userMapper.selectById(senderId);
        if (sender != null) {
            notification.setSenderId(senderId);
            notification.setSenderName(sender.getName() != null ? sender.getName() : sender.getUsername());
        }
        notification.setStatus("DRAFT");
        notificationMapper.insert(notification);
        logger.info("创建通知成功，ID: {}, 标题: {}", notification.getId(), notification.getTitle());
        return notification;
    }

    @Transactional
    public Notification updateNotification(Notification notification) {
        Notification existing = notificationMapper.selectById(notification.getId());
        if (existing == null) {
            throw new RuntimeException("通知不存在");
        }
        if ("PUBLISHED".equals(existing.getStatus())) {
            throw new RuntimeException("已发布的通知不能修改");
        }
        notificationMapper.update(notification);
        logger.info("更新通知成功，ID: {}", notification.getId());
        return notification;
    }

    @Transactional
    public void deleteNotification(Integer notificationId) {
        Notification notification = notificationMapper.selectById(notificationId);
        if (notification == null) {
            throw new RuntimeException("通知不存在");
        }
        notificationMapper.deleteById(notificationId);
        logger.info("删除通知成功，ID: {}", notificationId);
    }

    @Transactional
    public void publishNotification(Integer notificationId, List<Integer> targetUserIds) {
        Notification notification = notificationMapper.selectById(notificationId);
        if (notification == null) {
            throw new RuntimeException("通知不存在");
        }

        if (!notification.isValid()) {
            throw new RuntimeException("通知不在有效期内");
        }

        notification.setStatus("PUBLISHED");
        notification.setPublishTime(LocalDateTime.now());
        notificationMapper.update(notification);

        if (targetUserIds == null || targetUserIds.isEmpty()) {
            targetUserIds = getAllActiveUserIds();
        }

        List<UserNotification> userNotifications = new ArrayList<>();
        for (Integer userId : targetUserIds) {
            UserNotification un = new UserNotification(notificationId, userId);
            un.setChannel(determineChannel(userId, notification));
            userNotifications.add(un);
        }

        if (!userNotifications.isEmpty()) {
            userNotificationMapper.batchInsert(userNotifications);
        }

        channelService.sendNotifications(notification, targetUserIds);

        logger.info("发布通知成功，ID: {}, 目标用户数: {}", notificationId, targetUserIds.size());
    }

    @Transactional
    public void archiveNotification(Integer notificationId) {
        notificationMapper.updateStatus(notificationId, "ARCHIVED");
        logger.info("归档通知成功，ID: {}", notificationId);
    }

    public Notification getNotificationById(Integer notificationId) {
        return notificationMapper.selectById(notificationId);
    }

    public List<Notification> getAllNotifications() {
        return notificationMapper.selectAll();
    }

    public List<Notification> getNotificationsByStatus(String status) {
        return notificationMapper.selectByStatus(status);
    }

    public List<Notification> getValidNotifications() {
        return notificationMapper.selectValidNotifications();
    }

    public List<UserNotification> getUserNotifications(Integer userId) {
        return userNotificationMapper.selectByUserId(userId);
    }

    public List<UserNotification> getUnreadNotifications(Integer userId) {
        return userNotificationMapper.selectUnreadByUserId(userId);
    }

    public int getUnreadCount(Integer userId) {
        return userNotificationMapper.countUnread(userId);
    }

    public UserNotification getUserNotificationById(Integer userId, Integer id) {
        UserNotification un = userNotificationMapper.selectById(id);
        if (un != null && un.getUserId().equals(userId)) {
            return un;
        }
        return null;
    }

    @Transactional
    public void markAsRead(Integer userId, Integer notificationId) {
        userNotificationMapper.markAsRead(userId, notificationId);
        logger.info("标记通知为已读，用户ID: {}, 通知ID: {}", userId, notificationId);
    }

    @Transactional
    public void markAllAsRead(Integer userId) {
        userNotificationMapper.markAllAsRead(userId);
        logger.info("标记所有通知为已读，用户ID: {}", userId);
    }

    @Transactional
    public void deleteNotificationForUser(Integer userId, Integer notificationId) {
        userNotificationMapper.markAsDeleted(userId, notificationId);
        logger.info("删除用户通知，用户ID: {}, 通知ID: {}", userId, notificationId);
    }

    @Transactional
    public void batchMarkAsRead(Integer userId, List<Integer> notificationIds) {
        if (notificationIds != null && !notificationIds.isEmpty()) {
            userNotificationMapper.batchMarkAsRead(userId, notificationIds);
            logger.info("批量标记通知为已读，用户ID: {}, 数量: {}", userId, notificationIds.size());
        }
    }

    @Transactional
    public void batchDelete(Integer userId, List<Integer> notificationIds) {
        if (notificationIds != null && !notificationIds.isEmpty()) {
            userNotificationMapper.batchDelete(userId, notificationIds);
            logger.info("批量删除用户通知，用户ID: {}, 数量: {}", userId, notificationIds.size());
        }
    }

    public List<UserNotificationPreference> getUserPreferences(Integer userId) {
        List<UserNotificationPreference> preferences = preferenceMapper.selectByUserId(userId);
        if (preferences.isEmpty()) {
            preferences = createDefaultPreferences(userId);
        }
        return preferences;
    }

    @Transactional
    public void updateUserPreference(UserNotificationPreference preference) {
        UserNotificationPreference existing = preferenceMapper.selectByUserIdAndType(
                preference.getUserId(), preference.getNotificationType());

        if (existing != null) {
            preference.setId(existing.getId());
            preferenceMapper.update(preference);
        } else {
            preferenceMapper.insert(preference);
        }
        logger.info("更新用户通知偏好，用户ID: {}, 类型: {}", preference.getUserId(), preference.getNotificationType());
    }

    private List<UserNotificationPreference> createDefaultPreferences(Integer userId) {
        List<UserNotificationPreference> preferences = new ArrayList<>();

        String[] types = { "SYSTEM", "BUSINESS", "MARKETING" };
        for (String type : types) {
            UserNotificationPreference pref = new UserNotificationPreference(userId, type);
            if ("SYSTEM".equals(type)) {
                pref.setPriorityThreshold("LOW");
            } else if ("BUSINESS".equals(type)) {
                pref.setPriorityThreshold("MEDIUM");
                pref.setReceiveEmail(true);
            } else {
                pref.setPriorityThreshold("HIGH");
            }
            preferenceMapper.insert(pref);
            preferences.add(pref);
        }

        return preferences;
    }

    private String determineChannel(Integer userId, Notification notification) {
        UserNotificationPreference preference = preferenceMapper.selectByUserIdAndType(
                userId, notification.getNotificationType());

        if (preference == null) {
            return "SYSTEM";
        }

        if (preference.isInQuietTime()) {
            return "SYSTEM";
        }

        if (!preference.shouldReceiveNotification(notification.getPriority())) {
            return null;
        }

        if (preference.getReceiveSystem()) {
            return "SYSTEM";
        } else if (preference.getReceiveEmail()) {
            return "EMAIL";
        } else if (preference.getReceiveSms()) {
            return "SMS";
        }

        return "SYSTEM";
    }

    private List<Integer> getAllActiveUserIds() {
        List<User> users = userMapper.selectAll();
        List<Integer> userIds = new ArrayList<>();
        for (User user : users) {
            userIds.add(user.getUserId());
        }
        return userIds;
    }

    @Transactional
    public void archiveExpiredNotifications() {
        int count = notificationMapper.archiveExpiredNotifications();
        if (count > 0) {
            logger.info("归档过期通知，数量: {}", count);
        }
    }
}
