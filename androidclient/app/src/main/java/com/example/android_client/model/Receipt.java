package com.example.android_client.model;

import com.google.gson.annotations.SerializedName;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 票据实体类
 * 对应后端Receipt实体
 */
public class Receipt {
    private Integer id;

    @SerializedName("receiptNumber")
    private String receiptNumber;

    @SerializedName("userId")
    private Integer userId;

    @SerializedName("reporterId")
    private Integer reporterId;

    @SerializedName("projectId")
    private Integer projectId;

    @SerializedName("reporterName")
    private String reporterName;

    @SerializedName("projectName")
    private String projectName;

    @SerializedName("receiptDate")
    private Date receiptDate;

    private BigDecimal amount;

    private String type;

    @SerializedName("description")
    private String description;

    @SerializedName("filePath")
    private String filePath;

    @SerializedName("fileHash")
    private String fileHash;

    @SerializedName("ocrData")
    private String ocrData;

    @SerializedName("verificationStatus")
    private String verificationStatus;

    @SerializedName("verifiedBy")
    private Integer verifiedBy;

    @SerializedName("verifiedAt")
    private Date verifiedAt;

    @SerializedName("paymentStatus")
    private String paymentStatus;

    @SerializedName("paidAmount")
    private BigDecimal paidAmount;

    @SerializedName("paidAt")
    private Date paidAt;

    @SerializedName("paidBy")
    private Integer paidBy;

    @SerializedName("paymentMethod")
    private String paymentMethod;

    @SerializedName("createTime")
    private Date createTime;

    @SerializedName("updateTime")
    private Date updateTime;

    @SerializedName("financeStatus")
    private String financeStatus;

    @SerializedName("financeVerifiedBy")
    private Integer financeVerifiedBy;

    @SerializedName("financeVerifiedAt")
    private Date financeVerifiedAt;

    @SerializedName("financeComment")
    private String financeComment;

    @SerializedName("projectManagerStatus")
    private String projectManagerStatus;

    @SerializedName("projectManagerVerifiedBy")
    private Integer projectManagerVerifiedBy;

    @SerializedName("projectManagerVerifiedAt")
    private Date projectManagerVerifiedAt;

    @SerializedName("projectManagerComment")
    private String projectManagerComment;

    @SerializedName("managerStatus")
    private String managerStatus;

    @SerializedName("managerVerifiedBy")
    private Integer managerVerifiedBy;

    @SerializedName("managerVerifiedAt")
    private Date managerVerifiedAt;

    @SerializedName("managerComment")
    private String managerComment;

    @SerializedName("currentApprovalLevel")
    private Integer currentApprovalLevel;

    public Receipt() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getReceiptNumber() {
        return receiptNumber;
    }

    public void setReceiptNumber(String receiptNumber) {
        this.receiptNumber = receiptNumber;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Integer getReporterId() {
        return reporterId;
    }

    public void setReporterId(Integer reporterId) {
        this.reporterId = reporterId;
    }

    public Integer getProjectId() {
        return projectId;
    }

    public void setProjectId(Integer projectId) {
        this.projectId = projectId;
    }

    public String getReporterName() {
        return reporterName;
    }

    public void setReporterName(String reporterName) {
        this.reporterName = reporterName;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public Date getReceiptDate() {
        return receiptDate;
    }

    public void setReceiptDate(Date receiptDate) {
        this.receiptDate = receiptDate;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getFileHash() {
        return fileHash;
    }

    public void setFileHash(String fileHash) {
        this.fileHash = fileHash;
    }

    public String getOcrData() {
        return ocrData;
    }

    public void setOcrData(String ocrData) {
        this.ocrData = ocrData;
    }

    public String getVerificationStatus() {
        return verificationStatus;
    }

    public void setVerificationStatus(String verificationStatus) {
        this.verificationStatus = verificationStatus;
    }

    public Integer getVerifiedBy() {
        return verifiedBy;
    }

    public void setVerifiedBy(Integer verifiedBy) {
        this.verifiedBy = verifiedBy;
    }

    public Date getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(Date verifiedAt) {
        this.verifiedAt = verifiedAt;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public BigDecimal getPaidAmount() {
        return paidAmount;
    }

    public void setPaidAmount(BigDecimal paidAmount) {
        this.paidAmount = paidAmount;
    }

    public Date getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(Date paidAt) {
        this.paidAt = paidAt;
    }

    public Integer getPaidBy() {
        return paidBy;
    }

    public void setPaidBy(Integer paidBy) {
        this.paidBy = paidBy;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
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

    public String getFinanceStatus() {
        return financeStatus;
    }

    public void setFinanceStatus(String financeStatus) {
        this.financeStatus = financeStatus;
    }

    public Integer getFinanceVerifiedBy() {
        return financeVerifiedBy;
    }

    public void setFinanceVerifiedBy(Integer financeVerifiedBy) {
        this.financeVerifiedBy = financeVerifiedBy;
    }

    public Date getFinanceVerifiedAt() {
        return financeVerifiedAt;
    }

    public void setFinanceVerifiedAt(Date financeVerifiedAt) {
        this.financeVerifiedAt = financeVerifiedAt;
    }

    public String getFinanceComment() {
        return financeComment;
    }

    public void setFinanceComment(String financeComment) {
        this.financeComment = financeComment;
    }

    public String getProjectManagerStatus() {
        return projectManagerStatus;
    }

    public void setProjectManagerStatus(String projectManagerStatus) {
        this.projectManagerStatus = projectManagerStatus;
    }

    public Integer getProjectManagerVerifiedBy() {
        return projectManagerVerifiedBy;
    }

    public void setProjectManagerVerifiedBy(Integer projectManagerVerifiedBy) {
        this.projectManagerVerifiedBy = projectManagerVerifiedBy;
    }

    public Date getProjectManagerVerifiedAt() {
        return projectManagerVerifiedAt;
    }

    public void setProjectManagerVerifiedAt(Date projectManagerVerifiedAt) {
        this.projectManagerVerifiedAt = projectManagerVerifiedAt;
    }

    public String getProjectManagerComment() {
        return projectManagerComment;
    }

    public void setProjectManagerComment(String projectManagerComment) {
        this.projectManagerComment = projectManagerComment;
    }

    public String getManagerStatus() {
        return managerStatus;
    }

    public void setManagerStatus(String managerStatus) {
        this.managerStatus = managerStatus;
    }

    public Integer getManagerVerifiedBy() {
        return managerVerifiedBy;
    }

    public void setManagerVerifiedBy(Integer managerVerifiedBy) {
        this.managerVerifiedBy = managerVerifiedBy;
    }

    public Date getManagerVerifiedAt() {
        return managerVerifiedAt;
    }

    public void setManagerVerifiedAt(Date managerVerifiedAt) {
        this.managerVerifiedAt = managerVerifiedAt;
    }

    public String getManagerComment() {
        return managerComment;
    }

    public void setManagerComment(String managerComment) {
        this.managerComment = managerComment;
    }

    public Integer getCurrentApprovalLevel() {
        return currentApprovalLevel;
    }

    public void setCurrentApprovalLevel(Integer currentApprovalLevel) {
        this.currentApprovalLevel = currentApprovalLevel;
    }

    public String getStatusDisplay() {
        if (currentApprovalLevel == null || currentApprovalLevel == 0) {
            return "待财务审核";
        } else if (currentApprovalLevel == 1) {
            return "待项目经理审核";
        } else if (currentApprovalLevel == 2) {
            return "待经理审核";
        } else if (currentApprovalLevel == 3) {
            return "审核完成";
        }
        return "待审核";
    }

    public String getApprovalProgressText() {
        return getStatusDisplay();
    }

    @Override
    public String toString() {
        return "Receipt{" +
                "id=" + id +
                ", projectName='" + projectName + '\'' +
                ", amount=" + amount +
                ", type='" + type + '\'' +
                ", currentApprovalLevel=" + currentApprovalLevel +
                '}';
    }
}
