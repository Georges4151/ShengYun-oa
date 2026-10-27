package com.example.android_client.model;

import com.google.gson.annotations.SerializedName;

public class Notification {
    private Integer id;
    private String title;
    @SerializedName(value = "contentType", alternate = { "content_type" })
    private String contentType;
    private String content;
    @SerializedName(value = "linkUrl", alternate = { "link_url" })
    private String linkUrl;
    private String priority;
    @SerializedName(value = "notificationType", alternate = { "notification_type" })
    private String notificationType;
    @SerializedName(value = "senderId", alternate = { "sender_id" })
    private Integer senderId;
    @SerializedName(value = "senderName", alternate = { "sender_name" })
    private String senderName;
    @SerializedName(value = "validStartTime", alternate = { "valid_start_time" })
    private String validStartTime;
    @SerializedName(value = "validEndTime", alternate = { "valid_end_time" })
    private String validEndTime;
    private String status;
    @SerializedName(value = "publishTime", alternate = { "publish_time" })
    private String publishTime;
    @SerializedName(value = "createdAt", alternate = { "created_at" })
    private String createdAt;
    @SerializedName(value = "updatedAt", alternate = { "updated_at" })
    private String updatedAt;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getLinkUrl() {
        return linkUrl;
    }

    public void setLinkUrl(String linkUrl) {
        this.linkUrl = linkUrl;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getNotificationType() {
        return notificationType;
    }

    public void setNotificationType(String notificationType) {
        this.notificationType = notificationType;
    }

    public Integer getSenderId() {
        return senderId;
    }

    public void setSenderId(Integer senderId) {
        this.senderId = senderId;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getValidStartTime() {
        return validStartTime;
    }

    public void setValidStartTime(String validStartTime) {
        this.validStartTime = validStartTime;
    }

    public String getValidEndTime() {
        return validEndTime;
    }

    public void setValidEndTime(String validEndTime) {
        this.validEndTime = validEndTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPublishTime() {
        return publishTime;
    }

    public void setPublishTime(String publishTime) {
        this.publishTime = publishTime;
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

    public boolean isHighPriority() {
        return "HIGH".equals(this.priority);
    }

    public boolean isMediumPriority() {
        return "MEDIUM".equals(this.priority);
    }
}
