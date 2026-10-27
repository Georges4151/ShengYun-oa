package com.ceams.model.entity;

import java.util.Date;

public class ElectronicSealStamp {
    private Integer id;
    private Integer sealId;
    private String sealName;
    private Integer projectId;
    private String projectName;
    private String contractName;
    private String contractPath;
    private String stampPosition;
    private String stampImage;
    private Date stampTime;
    private Integer userId;
    private String userName;
    private String ipAddress;
    private Date createTime;
    private Date updateTime;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getSealId() {
        return sealId;
    }

    public void setSealId(Integer sealId) {
        this.sealId = sealId;
    }

    public String getSealName() {
        return sealName;
    }

    public void setSealName(String sealName) {
        this.sealName = sealName;
    }

    public Integer getProjectId() {
        return projectId;
    }

    public void setProjectId(Integer projectId) {
        this.projectId = projectId;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getContractName() {
        return contractName;
    }

    public void setContractName(String contractName) {
        this.contractName = contractName;
    }

    public String getContractPath() {
        return contractPath;
    }

    public void setContractPath(String contractPath) {
        this.contractPath = contractPath;
    }

    public String getStampPosition() {
        return stampPosition;
    }

    public void setStampPosition(String stampPosition) {
        this.stampPosition = stampPosition;
    }

    public String getStampImage() {
        return stampImage;
    }

    public void setStampImage(String stampImage) {
        this.stampImage = stampImage;
    }

    public Date getStampTime() {
        return stampTime;
    }

    public void setStampTime(Date stampTime) {
        this.stampTime = stampTime;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
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
}
