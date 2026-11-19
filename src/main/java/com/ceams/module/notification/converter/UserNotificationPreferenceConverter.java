package com.ceams.module.notification.converter;

import com.ceams.module.notification.entity.UserNotificationPreference;
import com.ceams.module.notification.vo.UserNotificationPreferenceVO;
import java.util.List;
import org.mapstruct.Mapper;

/** 用户通知偏好实体 <-> VO 转换。 */
@Mapper(componentModel = "spring")
public interface UserNotificationPreferenceConverter {

    default UserNotificationPreferenceVO toVO(UserNotificationPreference entity) {
        if (entity == null) {
            return null;
        }
        UserNotificationPreferenceVO vo = new UserNotificationPreferenceVO();
        vo.setId(entity.getId());
        vo.setUserId(entity.getUserId());
        vo.setNotificationType(entity.getNotificationType());
        vo.setReceiveSystem(entity.getReceiveSystem());
        vo.setReceiveEmail(entity.getReceiveEmail());
        vo.setReceiveSms(entity.getReceiveSms());
        vo.setQuietMode(entity.getQuietMode());
        vo.setQuietStartTime(entity.getQuietStartTime());
        vo.setQuietEndTime(entity.getQuietEndTime());
        vo.setPriorityThreshold(entity.getPriorityThreshold());
        return vo;
    }

    List<UserNotificationPreferenceVO> toVOList(List<UserNotificationPreference> entities);
}
