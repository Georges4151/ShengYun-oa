package com.ceams.controller;

import com.ceams.model.entity.Notification;
import com.ceams.model.entity.UserNotification;
import com.ceams.model.entity.UserNotificationPreference;
import com.ceams.service.NotificationService;
import com.ceams.service.UserSessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserSessionService userSessionService;

    @PostMapping
    public ResponseEntity<?> createNotification(@RequestBody Notification notification) {
        try {
            Integer currentUserId = userSessionService.getCurrentUserId();
            Notification created = notificationService.createNotification(notification, currentUserId);
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("创建通知失败：" + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateNotification(@PathVariable Integer id, @RequestBody Notification notification) {
        try {
            notification.setId(id);
            Notification updated = notificationService.updateNotification(notification);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("更新通知失败：" + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteNotification(@PathVariable Integer id) {
        try {
            notificationService.deleteNotification(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("删除通知失败：" + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getNotification(@PathVariable Integer id) {
        try {
            Notification notification = notificationService.getNotificationById(id);
            if (notification == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("通知不存在");
            }
            return ResponseEntity.ok(notification);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("获取通知失败：" + e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<?> getAllNotifications(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type) {
        try {
            List<Notification> notifications;
            if (status != null && !status.isEmpty()) {
                notifications = notificationService.getNotificationsByStatus(status);
            } else {
                notifications = notificationService.getAllNotifications();
            }
            return ResponseEntity.ok(notifications);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("获取通知列表失败：" + e.getMessage());
        }
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<?> publishNotification(
            @PathVariable Integer id,
            @RequestBody(required = false) Map<String, List<Integer>> request) {
        try {
            List<Integer> targetUserIds = request != null ? request.get("targetUserIds") : null;
            notificationService.publishNotification(id, targetUserIds);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("发布通知失败：" + e.getMessage());
        }
    }

    @PostMapping("/{id}/archive")
    public ResponseEntity<?> archiveNotification(@PathVariable Integer id) {
        try {
            notificationService.archiveNotification(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("归档通知失败：" + e.getMessage());
        }
    }

    @GetMapping("/user")
    public ResponseEntity<?> getUserNotifications() {
        try {
            Integer userId = userSessionService.getCurrentUserId();
            List<UserNotification> notifications = notificationService.getUserNotifications(userId);
            return ResponseEntity.ok(notifications);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("获取用户通知失败：" + e.getMessage());
        }
    }

    @GetMapping("/user/unread")
    public ResponseEntity<?> getUnreadNotifications() {
        try {
            Integer userId = userSessionService.getCurrentUserId();
            List<UserNotification> notifications = notificationService.getUnreadNotifications(userId);
            return ResponseEntity.ok(notifications);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("获取未读通知失败：" + e.getMessage());
        }
    }

    @GetMapping("/user/unread-count")
    public ResponseEntity<?> getUnreadCount() {
        try {
            Integer userId = userSessionService.getCurrentUserId();
            int count = notificationService.getUnreadCount(userId);
            return ResponseEntity.ok(count);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("获取未读数量失败：" + e.getMessage());
        }
    }

    @PostMapping("/user/read")
    public ResponseEntity<?> markAsRead(@RequestBody Map<String, Integer> request) {
        try {
            Integer userId = userSessionService.getCurrentUserId();
            Integer notificationId = request.get("notificationId");
            notificationService.markAsRead(userId, notificationId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("标记已读失败：" + e.getMessage());
        }
    }

    @PostMapping("/user/read-all")
    public ResponseEntity<?> markAllAsRead() {
        try {
            Integer userId = userSessionService.getCurrentUserId();
            notificationService.markAllAsRead(userId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("标记全部已读失败：" + e.getMessage());
        }
    }

    @DeleteMapping("/user")
    public ResponseEntity<?> deleteNotificationForUser(@RequestBody Map<String, Integer> request) {
        try {
            Integer userId = userSessionService.getCurrentUserId();
            Integer notificationId = request.get("notificationId");
            notificationService.deleteNotificationForUser(userId, notificationId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("删除通知失败：" + e.getMessage());
        }
    }

    @PostMapping("/user/delete")
    public ResponseEntity<?> deleteNotificationForUserPost(@RequestBody Map<String, Integer> request) {
        return deleteNotificationForUser(request);
    }

    @PostMapping("/user/batch-read")
    public ResponseEntity<?> batchMarkAsRead(@RequestBody Map<String, List<Integer>> request) {
        try {
            Integer userId = userSessionService.getCurrentUserId();
            List<Integer> notificationIds = request.get("notificationIds");
            notificationService.batchMarkAsRead(userId, notificationIds);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("批量标记已读失败：" + e.getMessage());
        }
    }

    @PostMapping("/user/batch-delete")
    public ResponseEntity<?> batchDelete(@RequestBody Map<String, List<Integer>> request) {
        try {
            Integer userId = userSessionService.getCurrentUserId();
            List<Integer> notificationIds = request.get("notificationIds");
            notificationService.batchDelete(userId, notificationIds);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("批量删除失败：" + e.getMessage());
        }
    }

    @GetMapping("/user/preferences")
    public ResponseEntity<?> getUserPreferences() {
        try {
            Integer userId = userSessionService.getCurrentUserId();
            List<UserNotificationPreference> preferences = notificationService.getUserPreferences(userId);
            return ResponseEntity.ok(preferences);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("获取通知偏好失败：" + e.getMessage());
        }
    }

    @PutMapping("/user/preferences")
    public ResponseEntity<?> updateUserPreference(@RequestBody UserNotificationPreference preference) {
        try {
            Integer userId = userSessionService.getCurrentUserId();
            preference.setUserId(userId);
            notificationService.updateUserPreference(preference);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("更新通知偏好失败：" + e.getMessage());
        }
    }
}
