package com.ceams.module.notification.service;

import com.ceams.module.notification.dto.UserNotificationPreferenceUpsertRequest;
import com.ceams.module.notification.vo.UserNotificationPreferenceVO;

import java.util.List;

/**
 * 用户通知偏好服务。
 */
public interface UserNotificationPreferenceService {

    List<UserNotificationPreferenceVO> getMine();

    String upsert(UserNotificationPreferenceUpsertRequest request);

    void delete(Long id);
}
