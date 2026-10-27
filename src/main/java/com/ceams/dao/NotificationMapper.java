package com.ceams.dao;

import com.ceams.model.entity.Notification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface NotificationMapper {

        int insert(Notification notification);

        int update(Notification notification);

        int deleteById(Integer id);

        Notification selectById(Integer id);

        List<Notification> selectAll();

        List<Notification> selectByStatus(@Param("status") String status);

        List<Notification> selectByType(@Param("notificationType") String notificationType);

        List<Notification> selectByPriority(@Param("priority") String priority);

        List<Notification> selectValidNotifications();

        List<Notification> selectWithPagination(
                        @Param("status") String status,
                        @Param("notificationType") String notificationType,
                        @Param("priority") String priority,
                        @Param("offset") int offset,
                        @Param("limit") int limit);

        int count(
                        @Param("status") String status,
                        @Param("notificationType") String notificationType,
                        @Param("priority") String priority);

        int updateStatus(
                        @Param("id") Integer id,
                        @Param("status") String status);

        int archiveExpiredNotifications();
}
