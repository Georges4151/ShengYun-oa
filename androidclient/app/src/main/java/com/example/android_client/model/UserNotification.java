package com.example.android_client.model;

import com.google.gson.annotations.SerializedName;

public class UserNotification {
    private Integer id;
    @SerializedName("notificationId")
    private Integer notificationId;
    @SerializedName("userId")
    private Integer userId;
    @SerializedName(value = "isRead", alternate = { "is_read" })
    private Boolean isRead;
    @SerializedName(value = "readTime", alternate = { "read_time" })
    private String readTime;
    @SerializedName(value = "isDeleted", alternate = { "is_deleted" })
    private Boolean isDeleted;
    private String channel;
    @SerializedName(value = "pushStatus", alternate = { "push_status" })
    private String pushStatus;
    @SerializedName(value = "pushTime", alternate = { "push_time" })
    private String pushTime;
    @SerializedName(value = "createdAt", alternate = { "created_at" })
    private String createdAt;
    @SerializedName(value = "updatedAt", alternate = { "updated_at" })
    private String updatedAt;
    private Notification notification;

    public UserNotification() {
        this.isRead = false;
        this.isDeleted = false;
        this.pushStatus = "PENDING";
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

    public String getReadTime() {
        return readTime;
    }

    public void setReadTime(String readTime) {
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

    public String getPushTime() {
        return pushTime;
    }

    public void setPushTime(String pushTime) {
        this.pushTime = pushTime;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Notification getNotification() {
        return notification;
    }

    public void setNotification(Notification notification) {
        this.notification = notification;
    }

    public boolean isUnread() {
        return this.isRead == null || !this.isRead;
    }

    public String getDisplayTitle() {
        return notification != null ? notification.getTitle() : "未知通知";
    }

    public String getDisplayContent() {
        return notification != null ? notification.getContent() : "";
    }

    public String getDisplayTime() {
        if (createdAt != null && createdAt.length() >= 16) {
            return createdAt.substring(0, 16).replace("T", " ");
        }
        return createdAt != null ? createdAt : "";
    }

    public int getPriorityColor() {
        if (notification == null)
            return 0xFF757575;
        String priority = notification.getPriority();
        if ("HIGH".equals(priority))
            return 0xFFF44336;
        if ("MEDIUM".equals(priority))
            return 0xFFFF9800;
        return 0xFF4CAF50;
    }
}
