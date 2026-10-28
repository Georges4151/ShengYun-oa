package com.ceams.module.notification.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.common.security.SecurityUtils;
import com.ceams.module.notification.converter.UserNotificationPreferenceConverter;
import com.ceams.module.notification.dto.UserNotificationPreferenceUpsertRequest;
import com.ceams.module.notification.entity.UserNotificationPreference;
import com.ceams.module.notification.mapper.UserNotificationPreferenceMapper;
import com.ceams.module.notification.service.UserNotificationPreferenceService;
import com.ceams.module.notification.vo.UserNotificationPreferenceVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 用户通知偏好服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserNotificationPreferenceServiceImpl implements UserNotificationPreferenceService {

    private final UserNotificationPreferenceMapper userNotificationPreferenceMapper;
    private final UserNotificationPreferenceConverter userNotificationPreferenceConverter;

    @Override
    public List<UserNotificationPreferenceVO> getMine() {
        Long userId = requireUserId();
        LambdaQueryWrapper<UserNotificationPreference> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserNotificationPreference::getUserId, userId);
        wrapper.orderByAsc(UserNotificationPreference::getNotificationType);
        return userNotificationPreferenceConverter.toVOList(userNotificationPreferenceMapper.selectList(wrapper));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String upsert(UserNotificationPreferenceUpsertRequest request) {
        Long userId = requireUserId();
        LambdaQueryWrapper<UserNotificationPreference> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserNotificationPreference::getUserId, userId);
        wrapper.eq(UserNotificationPreference::getNotificationType, request.getNotificationType());
        UserNotificationPreference existing = userNotificationPreferenceMapper.selectOne(wrapper);

        if (existing != null) {
            if (request.getReceiveSystem() != null) {
                existing.setReceiveSystem(request.getReceiveSystem());
            }
            if (request.getReceiveEmail() != null) {
                existing.setReceiveEmail(request.getReceiveEmail());
            }
            if (request.getReceiveSms() != null) {
                existing.setReceiveSms(request.getReceiveSms());
            }
            if (request.getQuietMode() != null) {
                existing.setQuietMode(request.getQuietMode());
            }
            if (request.getQuietStartTime() != null) {
                existing.setQuietStartTime(request.getQuietStartTime());
            }
            if (request.getQuietEndTime() != null) {
                existing.setQuietEndTime(request.getQuietEndTime());
            }
            if (request.getPriorityThreshold() != null) {
                existing.setPriorityThreshold(request.getPriorityThreshold());
            }
            userNotificationPreferenceMapper.updateById(existing);
            return String.valueOf(existing.getId());
        }

        UserNotificationPreference preference = new UserNotificationPreference();
        preference.setUserId(userId);
        preference.setNotificationType(request.getNotificationType());
        preference.setReceiveSystem(request.getReceiveSystem() == null ? 1 : request.getReceiveSystem());
        preference.setReceiveEmail(request.getReceiveEmail() == null ? 0 : request.getReceiveEmail());
        preference.setReceiveSms(request.getReceiveSms() == null ? 0 : request.getReceiveSms());
        preference.setQuietMode(request.getQuietMode() == null ? 0 : request.getQuietMode());
        preference.setQuietStartTime(request.getQuietStartTime());
        preference.setQuietEndTime(request.getQuietEndTime());
        preference.setPriorityThreshold(request.getPriorityThreshold());
        userNotificationPreferenceMapper.insert(preference);
        return String.valueOf(preference.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        UserNotificationPreference preference = userNotificationPreferenceMapper.selectById(id);
        if (preference == null) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "通知偏好不存在");
        }
        if (!requireUserId().equals(preference.getUserId())) {
            throw BusinessException.of(ErrorCode.AUTH_ACCESS_DENIED, "无权操作他人的通知偏好");
        }
        userNotificationPreferenceMapper.deleteById(id);
    }

    private Long requireUserId() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw BusinessException.of(ErrorCode.AUTH_ACCESS_DENIED, "未登录");
        }
        return userId;
    }
}
