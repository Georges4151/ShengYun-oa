package com.ceams.dao;

import com.ceams.model.entity.UserNotification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserNotificationMapper {

        int insert(UserNotification userNotification);

        int batchInsert(@Param("list") List<UserNotification> list);

        int update(UserNotification userNotification);

        int deleteById(Integer id);

        UserNotification selectById(Integer id);

        UserNotification selectByUserIdAndNotificationId(
                        @Param("userId") Integer userId,
                        @Param("notificationId") Integer notificationId);

        List<UserNotification> selectByUserId(@Param("userId") Integer userId);

        List<UserNotification> selectUnreadByUserId(@Param("userId") Integer userId);

        List<UserNotification> selectWithNotification(
                        @Param("userId") Integer userId,
                        @Param("isRead") Boolean isRead,
                        @Param("isDeleted") Boolean isDeleted,
                        @Param("offset") int offset,
                        @Param("limit") int limit);

        int count(
                        @Param("userId") Integer userId,
                        @Param("isRead") Boolean isRead,
                        @Param("isDeleted") Boolean isDeleted);

        int countUnread(@Param("userId") Integer userId);

        int markAsRead(
                        @Param("userId") Integer userId,
                        @Param("notificationId") Integer notificationId);

        int markAllAsRead(@Param("userId") Integer userId);

        int markAsDeleted(
                        @Param("userId") Integer userId,
                        @Param("notificationId") Integer notificationId);

        int batchMarkAsRead(
                        @Param("userId") Integer userId,
                        @Param("notificationIds") List<Integer> notificationIds);

        int batchDelete(
                        @Param("userId") Integer userId,
                        @Param("notificationIds") List<Integer> notificationIds);

        int updatePushStatus(
                        @Param("id") Integer id,
                        @Param("pushStatus") String pushStatus,
                        @Param("channel") String channel);
}
