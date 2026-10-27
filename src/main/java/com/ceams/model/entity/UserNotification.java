package com.ceams.model.entity;

import java.time.LocalDateTime;

public class UserNotification {
    private Integer id;
    private Integer notificationId;
    private Integer userId;
    private Boolean isRead;
    private LocalDateTime readTime;
    private Boolean isDeleted;
    private String channel;
    private String pushStatus;
    private LocalDateTime pushTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Notification notification;

    public UserNotification() {
        this.isRead = false;
        this.isDeleted = false;
        this.pushStatus = "PENDING";
    }

    public UserNotification(Integer notificationId, Integer userId) {
        this();
        this.notificationId = notificationId;
        this.userId = userId;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(Integer notificationId) {
        this.notificationId = notificationId;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Boolean getIsRead() {
        return isRead;
    }

    public void setIsRead(Boolean isRead) {
        this.isRead = isRead;
    }

    public LocalDateTime getReadTime() {
        return readTime;
    }

    public void setReadTime(LocalDateTime readTime) {
        this.readTime = readTime;
    }

    public Boolean getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(Boolean isDeleted) {
        this.isDeleted = isDeleted;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getPushStatus() {
        return pushStatus;
    }

    public void setPushStatus(String pushStatus) {
        this.pushStatus = pushStatus;
    }

    public LocalDateTime getPushTime() {
        return pushTime;
    }

    public void setPushTime(LocalDateTime pushTime) {
        this.pushTime = pushTime;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Notification getNotification() {
        return notification;
    }

    public void setNotification(Notification notification) {
        this.notification = notification;
    }

    public boolean isUnread() {
        return !this.isRead;
    }

    public boolean isPending() {
        return "PENDING".equals(this.pushStatus);
    }

    public boolean isSent() {
        return "SENT".equals(this.pushStatus);
    }

    public boolean isFailed() {
        return "FAILED".equals(this.pushStatus);
    }

    public void markAsRead() {
        this.isRead = true;
        this.readTime = LocalDateTime.now();
    }

    public void markAsDeleted() {
        this.isDeleted = true;
    }

    @Override
    public String toString() {
        return "UserNotification{" +
                "id=" + id +
                ", notificationId=" + notificationId +
                ", userId=" + userId +
                ", isRead=" + isRead +
                ", channel='" + channel + '\'' +
                ", pushStatus='" + pushStatus + '\'' +
                '}';
    }
}
