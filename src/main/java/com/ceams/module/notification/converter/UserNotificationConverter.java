package com.ceams.module.notification.converter;

import com.ceams.module.notification.entity.UserNotification;
import com.ceams.module.notification.vo.UserNotificationVO;
import java.util.List;
import org.mapstruct.Mapper;

/** 用户通知实体 <-> VO 转换。 */
@Mapper(componentModel = "spring")
public interface UserNotificationConverter {

    default UserNotificationVO toVO(UserNotification entity) {
        if (entity == null) {
            return null;
        }
        UserNotificationVO vo = new UserNotificationVO();
        vo.setId(entity.getId());
        vo.setNotificationId(entity.getNotificationId());
        vo.setUserId(entity.getUserId());
        vo.setIsRead(entity.getIsRead());
        vo.setReadTime(entity.getReadTime());
        vo.setChannel(entity.getChannel());
        vo.setPushStatus(entity.getPushStatus());
        vo.setPushTime(entity.getPushTime());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }

    List<UserNotificationVO> toVOList(List<UserNotification> entities);
}
