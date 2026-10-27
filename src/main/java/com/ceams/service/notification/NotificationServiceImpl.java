package com.ceams.service.notification;

import com.ceams.dao.UserMapper;
import com.ceams.model.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class NotificationServiceImpl implements NotificationService {

    @Autowired
    private EmailService emailService;

    @Autowired
    private UserMapper userMapper;

    @Override
    public void sendApprovalNotification(int userId, int receiptId, String status, String comment) {
        // 获取用户信息
        User user = userMapper.selectById(userId);
        if (user != null && user.getEmail() != null) {
            // 发送邮件通知
            emailService.sendApprovalNotification(user.getEmail(), receiptId, status, comment);
        }
    }

    @Override
    public void sendPendingApprovalNotification(int userId, int receiptId, String role) {
        // 获取用户信息
        User user = userMapper.selectById(userId);
        if (user != null && user.getEmail() != null) {
            // 发送邮件通知
            emailService.sendPendingApprovalNotification(user.getEmail(), receiptId, role);
        }
    }
}
