package com.ceams.module.notification.service;

import com.ceams.common.response.PageResult;
import com.ceams.module.notification.dto.NotificationCreateRequest;
import com.ceams.module.notification.dto.NotificationUpdateRequest;
import com.ceams.module.notification.enums.NotificationPriority;
import com.ceams.module.notification.enums.NotificationStatus;
import com.ceams.module.notification.enums.NotificationType;
import com.ceams.module.notification.vo.NotificationVO;
import java.time.LocalDateTime;
import java.util.Collection;

/** 通知服务。 */
public interface NotificationService {

    PageResult<NotificationVO> page(
            long page,
            long size,
            NotificationType type,
            NotificationStatus status,
            NotificationPriority priority,
            LocalDateTime startTime,
            LocalDateTime endTime);

    NotificationVO detail(Long id);

    String create(NotificationCreateRequest request);

    /**
     * 创建并直接发布通知，定向投递给指定用户（站内信渠道，幂等跳过已存在记录）。
     *
     * @param request 通知内容
     * @param targetUserIds 目标用户 ID 集合（去重、忽略空值）
     * @return 通知 ID 字符串
     */
    String createAndPublishToUsers(
            NotificationCreateRequest request, Collection<Long> targetUserIds);

    void update(Long id, NotificationUpdateRequest request);

    void delete(Long id);

    void publish(Long id);

    void revoke(Long id);
}
