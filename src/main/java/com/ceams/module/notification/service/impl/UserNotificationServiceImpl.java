package com.ceams.module.notification.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.common.security.SecurityUtils;
import com.ceams.module.notification.converter.UserNotificationConverter;
import com.ceams.module.notification.entity.UserNotification;
import com.ceams.module.notification.mapper.UserNotificationMapper;
import com.ceams.module.notification.service.UserNotificationService;
import com.ceams.module.notification.util.PageUtil;
import com.ceams.module.notification.vo.UserNotificationMineVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 用户通知服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserNotificationServiceImpl implements UserNotificationService {

    private final UserNotificationMapper userNotificationMapper;
    private final UserNotificationConverter userNotificationConverter;

    @Override
    public UserNotificationMineVO mine(long page, long size) {
        Long userId = requireUserId();
        Page<UserNotification> p = PageUtil.resolve(page, size);
        LambdaQueryWrapper<UserNotification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserNotification::getUserId, userId);
        wrapper.orderByDesc(UserNotification::getCreateTime);
        IPage<UserNotification> result = userNotificationMapper.selectPage(p, wrapper);

        UserNotificationMineVO vo = new UserNotificationMineVO();
        vo.setRecords(userNotificationConverter.toVOList(result.getRecords()));
        vo.setTotal(result.getTotal());
        vo.setPage(result.getCurrent());
        vo.setSize(result.getSize());
        vo.setUnreadCount(countUnread(userId));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void read(Long id) {
        Long userId = requireUserId();
        UserNotification userNotification = userNotificationMapper.selectById(id);
        if (userNotification == null) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "用户通知不存在");
        }
        if (!userId.equals(userNotification.getUserId())) {
            throw BusinessException.of(ErrorCode.AUTH_ACCESS_DENIED, "无权操作他人的通知");
        }
        if (userNotification.getIsRead() == null || userNotification.getIsRead() != 1) {
            userNotification.setIsRead(1);
            userNotification.setReadTime(LocalDateTime.now());
            userNotificationMapper.updateById(userNotification);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void readAll() {
        Long userId = requireUserId();
        LambdaUpdateWrapper<UserNotification> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(UserNotification::getUserId, userId)
                .eq(UserNotification::getIsRead, 0)
                .set(UserNotification::getIsRead, 1)
                .set(UserNotification::getReadTime, LocalDateTime.now())
                .set(UserNotification::getUpdateTime, LocalDateTime.now())
                .set(UserNotification::getUpdateBy, userId);
        userNotificationMapper.update(null, wrapper);
    }

    @Override
    public long unreadCount() {
        return countUnread(requireUserId());
    }

    private long countUnread(Long userId) {
        LambdaQueryWrapper<UserNotification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserNotification::getUserId, userId);
        wrapper.eq(UserNotification::getIsRead, 0);
        return userNotificationMapper.selectCount(wrapper);
    }

    private Long requireUserId() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw BusinessException.of(ErrorCode.AUTH_ACCESS_DENIED, "未登录");
        }
        return userId;
    }
}
