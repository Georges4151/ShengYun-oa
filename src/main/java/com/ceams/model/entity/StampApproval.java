package com.ceams.model.entity;

import java.util.Date;

public class StampApproval {
    private Integer id;
    private Integer stampId;
    private String contractName;
    private String contractPath;
    private String originalContractPath;
    private String stampPositions;
    private Integer sealId;
    private String sealName;
    private String sealCode;
    private String sealPermission;
    private Integer projectId;
    private String projectName;
    private Integer applicantId;
    private String applicantName;
    private Integer approverId;
    private String approverName;
    private String status;
    private String rejectReason;
    private Date applyTime;
    private Date approvalTime;
    private Date createTime;
    private Date updateTime;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getStampId() {
        return stampId;
    }

    public void setStampId(Integer stampId) {
        this.stampId = stampId;
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

    public String getOriginalContractPath() {
        return originalContractPath;
    }

    public void setOriginalContractPath(String originalContractPath) {
        this.originalContractPath = originalContractPath;
    }

    public String getStampPositions() {
        return stampPositions;
    }

    public void setStampPositions(String stampPositions) {
        this.stampPositions = stampPositions;
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

    public String getSealCode() {
        return sealCode;
    }

    public void setSealCode(String sealCode) {
        this.sealCode = sealCode;
    }

    public String getSealPermission() {
        return sealPermission;
    }

    public void setSealPermission(String sealPermission) {
        this.sealPermission = sealPermission;
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

    public Integer getApplicantId() {
        return applicantId;
    }

    public void setApplicantId(Integer applicantId) {
        this.applicantId = applicantId;
    }

    public String getApplicantName() {
        return applicantName;
    }

    public void setApplicantName(String applicantName) {
        this.applicantName = applicantName;
    }

    public Integer getApproverId() {
        return approverId;
    }

    public void setApproverId(Integer approverId) {
        this.approverId = approverId;
    }

    public String getApproverName() {
        return approverName;
    }

    public void setApproverName(String approverName) {
        this.approverName = approverName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRejectReason() {
        return rejectReason;
    }

    public void setRejectReason(String rejectReason) {
        this.rejectReason = rejectReason;
    }

    public Date getApplyTime() {
        return applyTime;
    }

    public void setApplyTime(Date applyTime) {
        this.applyTime = applyTime;
    }

    public Date getApprovalTime() {
        return approvalTime;
    }

    public void setApprovalTime(Date approvalTime) {
        this.approvalTime = approvalTime;
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

    public String getStatusText() {
        if (status == null) {
            return "待审批";
        }
        switch (status) {
            case "pending":
                return "待审批";
            case "approved":
                return "已通过";
            case "rejected":
                return "已驳回";
            default:
                return status;
        }
    }

    public String getStatusStyle() {
        if (status == null) {
            return "-fx-text-fill: #f39c12;";
        }
        switch (status) {
            case "pending":
                return "-fx-text-fill: #f39c12;";
            case "approved":
                return "-fx-text-fill: #27ae60;";
            case "rejected":
                return "-fx-text-fill: #e74c3c;";
            default:
                return "-fx-text-fill: #7f8c8d;";
        }
    }
}
