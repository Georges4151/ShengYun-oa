package com.example.android_client.model;

/**
 * 审批请求类
 * 用于提交审批操作
 */
public class ApprovalRequest {
    private Integer receiptId;
    private String status;
    private String comment;

    public ApprovalRequest() {
    }

    public ApprovalRequest(Integer receiptId, String status, String comment) {
        this.receiptId = receiptId;
        this.status = status;
        this.comment = comment;
    }

    public Integer getReceiptId() {
        return receiptId;
    }

    public void setReceiptId(Integer receiptId) {
        this.receiptId = receiptId;
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

    @Override
    public String toString() {
        return "ApprovalRequest{" +
                "receiptId=" + receiptId +
                ", status='" + status + '\'' +
                ", comment='" + comment + '\'' +
                '}';
    }
}
