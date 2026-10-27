package com.ceams.service.notification;

public interface NotificationService {

    /**
     * 发送审批通知
     * 
     * @param userId    用户ID
     * @param receiptId 票据ID
     * @param status    审批状态
     * @param comment   审批意见
     */
    void sendApprovalNotification(int userId, int receiptId, String status, String comment);

    /**
     * 发送待审批通知
     * 
     * @param userId    用户ID
     * @param receiptId 票据ID
     * @param role      审批角色
     */
    void sendPendingApprovalNotification(int userId, int receiptId, String role);
}
