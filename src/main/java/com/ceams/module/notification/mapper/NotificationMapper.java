package com.ceams.module.notification.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ceams.module.notification.entity.Notification;
import org.apache.ibatis.annotations.Mapper;

/**
 * 通知 Mapper。
 */
@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {
}
