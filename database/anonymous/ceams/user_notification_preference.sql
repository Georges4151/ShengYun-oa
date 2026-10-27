create table user_notification_preference
(
    id                 int auto_increment
        primary key,
    user_id            int                                   not null,
    notification_type  varchar(50)                           not null,
    receive_system     tinyint(1)  default 1                 null,
    receive_email      tinyint(1)  default 0                 null,
    receive_sms        tinyint(1)  default 0                 null,
    quiet_mode         tinyint(1)  default 0                 null,
    quiet_start_time   time                                  null,
    quiet_end_time     time                                  null,
    priority_threshold varchar(10) default 'LOW'             null,
    created_at         datetime    default CURRENT_TIMESTAMP null,
    updated_at         datetime    default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP,
    constraint uk_user_type
        unique (user_id, notification_type)
)
    charset = utf8mb4;

