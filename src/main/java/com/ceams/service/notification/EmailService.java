package com.ceams.service.notification;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    /**
     * 发送邮件通知
     * 
     * @param to      收件人邮箱
     * @param subject 邮件主题
     * @param text    邮件内容
     */
    public void sendEmail(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        message.setFrom("noreply@ceams.com");
        mailSender.send(message);
    }

    /**
     * 发送审批通知邮件
     * 
     * @param to        收件人邮箱
     * @param receiptId 票据ID
     * @param status    审批状态
     * @param comment   审批意见
     */
    public void sendApprovalNotification(String to, int receiptId, String status, String comment) {
        String subject = "【CEAMS】票据审批通知";
        String text = String.format("您好，\n\n" +
                "您的票据（ID: %d）审批状态已更新：\n" +
                "状态：%s\n" +
                "意见：%s\n\n" +
                "如有疑问，请联系系统管理员。\n" +
                "CEAMS系统",
                receiptId, status, comment);
        sendEmail(to, subject, text);
    }

    /**
     * 发送待审批通知邮件
     * 
     * @param to        收件人邮箱
     * @param receiptId 票据ID
     * @param role      审批角色
     */
    public void sendPendingApprovalNotification(String to, int receiptId, String role) {
        String subject = "【CEAMS】待审批通知";
        String text = String.format("您好，\n\n" +
                "有一张票据（ID: %d）需要您的审批：\n" +
                "审批角色：%s\n\n" +
                "请登录系统进行审批操作。\n" +
                "CEAMS系统",
                receiptId, role);
        sendEmail(to, subject, text);
    }
}
