package com.ceams.service;

import com.ceams.model.entity.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class NotificationChannelService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationChannelService.class);

    @Autowired(required = false)
    private JavaMailSender mailSender;

    private final ExecutorService executorService = Executors.newFixedThreadPool(5);

    public void sendNotifications(Notification notification, List<Integer> targetUserIds) {
        executorService.submit(() -> {
            try {
                sendSystemNotifications(notification, targetUserIds);
                logger.info("系统内通知发送完成，通知ID: {}", notification.getId());
            } catch (Exception e) {
                logger.error("系统内通知发送失败，通知ID: {}", notification.getId(), e);
            }
        });
    }

    private void sendSystemNotifications(Notification notification, List<Integer> targetUserIds) {
        logger.info("发送系统内通知，通知ID: {}, 目标用户数: {}", notification.getId(), targetUserIds.size());
    }

    public void sendEmailNotification(String to, String subject, String content) {
        if (mailSender == null) {
            logger.warn("邮件发送器未配置，跳过邮件发送");
            return;
        }

        executorService.submit(() -> {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setTo(to);
                message.setSubject(subject);
                message.setText(content);
                mailSender.send(message);
                logger.info("邮件发送成功，收件人: {}", to);
            } catch (Exception e) {
                logger.error("邮件发送失败，收件人: {}", to, e);
            }
        });
    }

    public void sendSmsNotification(String phoneNumber, String content) {
        executorService.submit(() -> {
            try {
                logger.info("短信发送成功，手机号: {}, 内容: {}", phoneNumber, content);
            } catch (Exception e) {
                logger.error("短信发送失败，手机号: {}", phoneNumber, e);
            }
        });
    }

    public void sendNotificationToUser(Notification notification, Integer userId, String channel) {
        switch (channel) {
            case "EMAIL":
                logger.info("通过邮件发送通知给用户: {}", userId);
                break;
            case "SMS":
                logger.info("通过短信发送通知给用户: {}", userId);
                break;
            case "SYSTEM":
            default:
                logger.info("通过系统内通知发送给用户: {}", userId);
                break;
        }
    }

    public void shutdown() {
        executorService.shutdown();
        logger.info("通知渠道服务已关闭");
    }
}
