package com.ceams.model.entity;

import java.math.BigDecimal;
import java.util.Date;

public class Receipt {
    private Integer id;

    private String receiptNumber;

    private Integer userId;

    private Integer reporterId;

    private Integer projectId;

    private String reporterName;

    private String projectName;

    private Date receiptDate;

    private BigDecimal amount;

    private String type;

    private String description;

    private String filePath;

    private String fileHash;

    private String ocrData;

    private String verificationStatus;

    private Integer verifiedBy;

    private Date verifiedAt;

    private String paymentStatus;

    private BigDecimal paidAmount;

    private Date paidAt;

    private Integer paidBy;

    private String paymentMethod;

    private Date createTime;

    private Date updateTime;

    private String financeStatus;
    private Integer financeVerifiedBy;
    private Date financeVerifiedAt;
    private String financeComment;

    private String projectManagerStatus;
    private Integer projectManagerVerifiedBy;
    private Date projectManagerVerifiedAt;
    private String projectManagerComment;

    private String managerStatus;
    private Integer managerVerifiedBy;
    private Date managerVerifiedAt;
    private String managerComment;

    private Integer currentApprovalLevel;

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

    // 获取中文验证状态
    public String getVerificationStatusText() {
        if (verificationStatus == null) {
            return "未验证";
        }
        switch (verificationStatus) {
            case "verified":
                return "已验证";
            case "rejected":
                return "已拒绝";
            case "pending":
                return "待验证";
            default:
                return verificationStatus;
        }
    }

    // 获取中文支付状态
    public String getPaymentStatusText() {
        if (paymentStatus == null) {
            return "未支付";
        }
        switch (paymentStatus) {
            case "paid":
                return "已支付";
            case "unpaid":
                return "未支付";
            case "partially_paid":
                return "部分支付";
            default:
                return paymentStatus;
        }
    }

    // 获取中文支付方式
    public String getPaymentMethodText() {
        if (paymentMethod == null) {
            return "未指定";
        }
        switch (paymentMethod) {
            case "cash":
                return "现金";
            case "bank_transfer":
                return "银行转账";
            case "wechat":
                return "微信";
            case "alipay":
                return "支付宝";
            case "other":
                return "其他";
            default:
                return paymentMethod;
        }
    }

    // 计算剩余金额
    public BigDecimal getRemainingAmount() {
        if (amount == null) {
            return BigDecimal.ZERO;
        }
        if (paidAmount == null) {
            return amount;
        }
        return amount.subtract(paidAmount);
    }

    // 三级审核相关字段的Getter和Setter
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

    // 获取三级审核状态的中文显示
    public String getFinanceStatusText() {
        if (financeStatus == null) {
            return "待财务审核";
        }
        switch (financeStatus) {
            case "approved":
                return "财务审核通过";
            case "rejected":
                return "财务审核拒绝";
            case "pending":
                return "待财务审核";
            default:
                return financeStatus;
        }
    }

    public String getProjectManagerStatusText() {
        if (projectManagerStatus == null) {
            return "待项目经理审核";
        }
        switch (projectManagerStatus) {
            case "approved":
                return "项目经理审核通过";
            case "rejected":
                return "项目经理审核拒绝";
            case "pending":
                return "待项目经理审核";
            default:
                return projectManagerStatus;
        }
    }

    public String getManagerStatusText() {
        if (managerStatus == null) {
            return "待经理审核";
        }
        switch (managerStatus) {
            case "approved":
                return "经理审核通过";
            case "rejected":
                return "经理审核拒绝";
            case "pending":
                return "待经理审核";
            default:
                return managerStatus;
        }
    }

    // 获取当前审核进度的中文描述
    public String getApprovalProgressText() {
        if (currentApprovalLevel == null || currentApprovalLevel == 0) {
            return "待财务审核";
        } else if (currentApprovalLevel == 1) {
            return "待项目经理审核";
        } else if (currentApprovalLevel == 2) {
            return "待经理审核";
        } else if (currentApprovalLevel == 3) {
            return "审核完成";
        }
        return "待财务审核";
    }
}