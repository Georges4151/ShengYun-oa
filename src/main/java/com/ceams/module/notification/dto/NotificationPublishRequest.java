package com.ceams.module.notification.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;

/** 通知发布请求：发布时指定接收范围。 */
@Data
public class NotificationPublishRequest {

    /** 接收范围：ALL/ROLES/USERS。 */
    @NotNull(message = "接收范围不能为空")
    private NotificationPublishScope scope;

    /** 接收角色 ID 列表（scope=ROLES 时必填）。 */
    private List<Long> roleIds;

    /** 接收用户 ID 列表（scope=USERS 时必填）。 */
    private List<Long> userIds;
}
