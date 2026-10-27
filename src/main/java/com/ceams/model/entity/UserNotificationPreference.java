package com.ceams.model.entity;

import java.time.LocalDateTime;
import java.time.LocalTime;

public class UserNotificationPreference {
    private Integer id;
    private Integer userId;
    private String notificationType;
    private Boolean receiveSystem;
    private Boolean receiveEmail;
    private Boolean receiveSms;
    private Boolean quietMode;
    private LocalTime quietStartTime;
    private LocalTime quietEndTime;
    private String priorityThreshold;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public UserNotificationPreference() {
        this.receiveSystem = true;
        this.receiveEmail = false;
        this.receiveSms = false;
        this.quietMode = false;
        this.priorityThreshold = "LOW";
    }

    public UserNotificationPreference(Integer userId, String notificationType) {
        this();
        this.userId = userId;
        this.notificationType = notificationType;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getNotificationType() {
        return notificationType;
    }

    public void setNotificationType(String notificationType) {
        this.notificationType = notificationType;
    }

    public Boolean getReceiveSystem() {
        return receiveSystem;
    }

    public void setReceiveSystem(Boolean receiveSystem) {
        this.receiveSystem = receiveSystem;
    }

    public Boolean getReceiveEmail() {
        return receiveEmail;
    }

    public void setReceiveEmail(Boolean receiveEmail) {
        this.receiveEmail = receiveEmail;
    }

    public Boolean getReceiveSms() {
        return receiveSms;
    }

    public void setReceiveSms(Boolean receiveSms) {
        this.receiveSms = receiveSms;
    }

    public Boolean getQuietMode() {
        return quietMode;
    }

    public void setQuietMode(Boolean quietMode) {
        this.quietMode = quietMode;
    }

    public LocalTime getQuietStartTime() {
        return quietStartTime;
    }

    public void setQuietStartTime(LocalTime quietStartTime) {
        this.quietStartTime = quietStartTime;
    }

    public LocalTime getQuietEndTime() {
        return quietEndTime;
    }

    public void setQuietEndTime(LocalTime quietEndTime) {
        this.quietEndTime = quietEndTime;
    }

    public String getPriorityThreshold() {
        return priorityThreshold;
    }

    public void setPriorityThreshold(String priorityThreshold) {
        this.priorityThreshold = priorityThreshold;
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

    public boolean isInQuietTime() {
        if (!quietMode || quietStartTime == null || quietEndTime == null) {
            return false;
        }

        LocalTime now = LocalTime.now();
        if (quietStartTime.isBefore(quietEndTime)) {
            return now.isAfter(quietStartTime) && now.isBefore(quietEndTime);
        } else {
            return now.isAfter(quietStartTime) || now.isBefore(quietEndTime);
        }
    }

    public boolean shouldReceiveNotification(String priority) {
        if (isInQuietTime()) {
            return false;
        }

        int priorityLevel = getPriorityLevel(priority);
        int thresholdLevel = getPriorityLevel(this.priorityThreshold);

        return priorityLevel >= thresholdLevel;
    }

    private int getPriorityLevel(String priority) {
        if (priority == null) {
            return 1;
        }
        switch (priority) {
            case "HIGH":
                return 3;
            case "MEDIUM":
                return 2;
            case "LOW":
            default:
                return 1;
        }
    }

    @Override
    public String toString() {
        return "UserNotificationPreference{" +
                "id=" + id +
                ", userId=" + userId +
                ", notificationType='" + notificationType + '\'' +
                ", receiveSystem=" + receiveSystem +
                ", receiveEmail=" + receiveEmail +
                ", receiveSms=" + receiveSms +
                ", quietMode=" + quietMode +
                '}';
    }
}
