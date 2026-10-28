package com.ceams.module.notification.service;

import com.ceams.common.response.PageResult;
import com.ceams.module.notification.dto.NotificationCreateRequest;
import com.ceams.module.notification.dto.NotificationUpdateRequest;
import com.ceams.module.notification.enums.NotificationPriority;
import com.ceams.module.notification.enums.NotificationStatus;
import com.ceams.module.notification.enums.NotificationType;
import com.ceams.module.notification.vo.NotificationVO;

import java.time.LocalDateTime;

/**
 * 通知服务。
 */
public interface NotificationService {

    PageResult<NotificationVO> page(long page, long size, NotificationType type, NotificationStatus status,
                                    NotificationPriority priority, LocalDateTime startTime, LocalDateTime endTime);

    NotificationVO detail(Long id);

    String create(NotificationCreateRequest request);

    void update(Long id, NotificationUpdateRequest request);

    void delete(Long id);

    void publish(Long id);

    void revoke(Long id);
}
