package com.ceams.dao;

import com.ceams.model.entity.UserNotificationPreference;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserNotificationPreferenceMapper {

    int insert(UserNotificationPreference preference);

    int batchInsert(@Param("list") List<UserNotificationPreference> list);

    int update(UserNotificationPreference preference);

    int deleteById(Integer id);

    UserNotificationPreference selectById(Integer id);

    UserNotificationPreference selectByUserIdAndType(
            @Param("userId") Integer userId,
            @Param("notificationType") String notificationType);

    List<UserNotificationPreference> selectByUserId(@Param("userId") Integer userId);

    int deleteByUserId(@Param("userId") Integer userId);
}
