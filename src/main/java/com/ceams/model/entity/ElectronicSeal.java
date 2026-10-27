package com.ceams.model.entity;

import java.util.Date;

public class ElectronicSeal {
    private Integer id;
    private String sealName;
    private String sealType;
    private String description;
    private String status;
    private String sealImage;
    private Date createTime;
    private Date updateTime;

    private String storageType;
    private String sealFileId;
    private String usbDriveId;
    private Boolean passwordProtected;
    private Date lastUsedTime;
    private Integer usageCount;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getSealName() {
        return sealName;
    }

    public void setSealName(String sealName) {
        this.sealName = sealName;
    }

    public String getSealType() {
        return sealType;
    }

    public void setSealType(String sealType) {
        this.sealType = sealType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSealImage() {
        return sealImage;
    }

    public void setSealImage(String sealImage) {
        this.sealImage = sealImage;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public String getStorageType() {
        return storageType;
    }

    public void setStorageType(String storageType) {
        this.storageType = storageType;
    }

    public String getSealFileId() {
        return sealFileId;
    }

    public void setSealFileId(String sealFileId) {
        this.sealFileId = sealFileId;
    }

    public String getUsbDriveId() {
        return usbDriveId;
    }

    public void setUsbDriveId(String usbDriveId) {
        this.usbDriveId = usbDriveId;
    }

    public Boolean getPasswordProtected() {
        return passwordProtected;
    }

    public void setPasswordProtected(Boolean passwordProtected) {
        this.passwordProtected = passwordProtected;
    }

    public Date getLastUsedTime() {
        return lastUsedTime;
    }

    public void setLastUsedTime(Date lastUsedTime) {
        this.lastUsedTime = lastUsedTime;
    }

    public Integer getUsageCount() {
        return usageCount;
    }

    public void setUsageCount(Integer usageCount) {
        this.usageCount = usageCount;
    }

    public String getStatusText() {
        if (status == null) {
            return "未启用";
        }
        switch (status) {
            case "active":
                return "启用";
            case "inactive":
                return "未启用";
            case "expired":
                return "已过期";
            default:
                return status;
        }
    }

    public String getStorageTypeText() {
        if (storageType == null || "local".equals(storageType)) {
            return "本地存储";
        } else if ("usb".equals(storageType)) {
            return "U盘存储";
        }
        return storageType;
    }

    public boolean isUsbStorage() {
        return "usb".equals(storageType);
    }
}
