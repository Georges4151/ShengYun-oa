package com.ceams.module.notification.service;

import com.ceams.module.notification.vo.UserNotificationMineVO;

/** 用户通知服务。 */
public interface UserNotificationService {

    UserNotificationMineVO mine(long page, long size);

    void read(Long id);

    void readAll();

    long unreadCount();
}
