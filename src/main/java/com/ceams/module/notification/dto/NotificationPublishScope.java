package com.ceams.module.notification.dto;

/** 通知发布接收范围。 */
public enum NotificationPublishScope {

    /** 全员：所有启用（ACTIVE、未删除）用户。 */
    ALL,

    /** 指定角色：按角色关联的用户。 */
    ROLES,

    /** 指定用户：显式指定的用户。 */
    USERS
}
