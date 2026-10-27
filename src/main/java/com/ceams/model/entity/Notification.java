package com.ceams.model.entity;

import java.time.LocalDateTime;

public class Notification {
    private Integer id;
    private String title;
    private String contentType;
    private String content;
    private String linkUrl;
    private String priority;
    private String notificationType;
    private Integer senderId;
    private String senderName;
    private LocalDateTime validStartTime;
    private LocalDateTime validEndTime;
    private String status;
    private LocalDateTime publishTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Notification() {
    }

    public Notification(String title, String contentType, String content, String priority, String notificationType) {
        this.title = title;
        this.contentType = contentType;
        this.content = content;
        this.priority = priority;
        this.notificationType = notificationType;
        this.status = "DRAFT";
    }

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

    public LocalDateTime getValidStartTime() {
        return validStartTime;
    }

    public void setValidStartTime(LocalDateTime validStartTime) {
        this.validStartTime = validStartTime;
    }

    public LocalDateTime getValidEndTime() {
        return validEndTime;
    }

    public void setValidEndTime(LocalDateTime validEndTime) {
        this.validEndTime = validEndTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getPublishTime() {
        return publishTime;
    }

    public void setPublishTime(LocalDateTime publishTime) {
        this.publishTime = publishTime;
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

    public boolean isHighPriority() {
        return "HIGH".equals(this.priority);
    }

    public boolean isMediumPriority() {
        return "MEDIUM".equals(this.priority);
    }

    public boolean isLowPriority() {
        return "LOW".equals(this.priority);
    }

    public boolean isPublished() {
        return "PUBLISHED".equals(this.status);
    }

    public boolean isDraft() {
        return "DRAFT".equals(this.status);
    }

    public boolean isArchived() {
        return "ARCHIVED".equals(this.status);
    }

    public boolean isValid() {
        LocalDateTime now = LocalDateTime.now();
        if (validStartTime != null && now.isBefore(validStartTime)) {
            return false;
        }
        if (validEndTime != null && now.isAfter(validEndTime)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "Notification{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", contentType='" + contentType + '\'' +
                ", priority='" + priority + '\'' +
                ", notificationType='" + notificationType + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
