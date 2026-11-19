package com.ceams.module.notification.converter;

import com.ceams.module.notification.entity.Notification;
import com.ceams.module.notification.vo.NotificationVO;
import java.util.List;
import org.mapstruct.Mapper;

/** 通知实体 <-> VO 转换。 */
@Mapper(componentModel = "spring")
public interface NotificationConverter {

    default NotificationVO toVO(Notification entity) {
        if (entity == null) {
            return null;
        }
        NotificationVO vo = new NotificationVO();
        vo.setId(entity.getId());
        vo.setTitle(entity.getTitle());
        vo.setContentType(entity.getContentType());
        vo.setContent(entity.getContent());
        vo.setLinkUrl(entity.getLinkUrl());
        vo.setPriority(entity.getPriority());
        vo.setNotificationType(entity.getNotificationType());
        vo.setSenderId(entity.getSenderId());
        vo.setSenderName(entity.getSenderName());
        vo.setValidStartTime(entity.getValidStartTime());
        vo.setValidEndTime(entity.getValidEndTime());
        vo.setStatus(entity.getStatus());
        vo.setPublishTime(entity.getPublishTime());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }

    List<NotificationVO> toVOList(List<Notification> entities);
}
