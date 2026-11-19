package com.ceams.module.notification.service;

import com.ceams.module.notification.enums.NotificationType;
import com.ceams.module.notification.vo.UserNotificationMineVO;

/** 用户通知服务。 */
public interface UserNotificationService {

    /** 「我的通知」分页（不按类型过滤，兼容旧行为）。 */
    UserNotificationMineVO mine(long page, long size);

    /**
     * 「我的通知」分页，可按通知类型过滤。
     *
     * @param page 页码（从 1 开始）
     * @param size 每页条数
     * @param type 通知类型，为空表示全部类型
     * @return 分页结果（含未读数）
     */
    UserNotificationMineVO mine(long page, long size, NotificationType type);

    void read(Long id);

    void readAll();

    long unreadCount();
}
