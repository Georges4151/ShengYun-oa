package com.example.android_client.model;

import com.google.gson.annotations.SerializedName;

import java.util.Date;

/**
 * 审批记录实体类
 * 对应后端ApprovalRecord实体
 */
public class ApprovalRecord {
    private Integer id;

    @SerializedName("receiptId")
    private Integer receiptId;

    @SerializedName("approvalLevel")
    private Integer approvalLevel;

    @SerializedName("approverRole")
    private String approverRole;

    @SerializedName("approverId")
    private Integer approverId;

    @SerializedName("approverName")
    private String approverName;

    private String status;

    private String comment;

    @SerializedName("approvalTime")
    private Date approvalTime;

    @SerializedName("createTime")
    private Date createTime;

    @SerializedName("updateTime")
    private Date updateTime;

    public ApprovalRecord() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getReceiptId() {
        return receiptId;
    }

    public void setReceiptId(Integer receiptId) {
        this.receiptId = receiptId;
    }

    public Integer getApprovalLevel() {
        return approvalLevel;
    }

    public void setApprovalLevel(Integer approvalLevel) {
        this.approvalLevel = approvalLevel;
    }

    public String getApproverRole() {
        return approverRole;
    }

    public void setApproverRole(String approverRole) {
        this.approverRole = approverRole;
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

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
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
            return "待审核";
        }
        switch (status) {
            case "approved":
                return "审核通过";
            case "rejected":
                return "审核拒绝";
            case "pending":
                return "待审核";
            default:
                return status;
        }
    }

    public String getApprovalLevelText() {
        if (approvalLevel == null) {
            return "未知";
        }
        switch (approvalLevel) {
            case 1:
                return "财务审核";
            case 2:
                return "项目经理审核";
            case 3:
                return "经理审核";
            default:
                return "未知";
        }
    }

    @Override
    public String toString() {
        return "ApprovalRecord{" +
                "id=" + id +
                ", receiptId=" + receiptId +
                ", approverName='" + approverName + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
