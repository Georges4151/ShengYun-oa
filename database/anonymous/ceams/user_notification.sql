create table user_notification
(
    id              int auto_increment
        primary key,
    notification_id int                                   not null,
    user_id         int                                   not null,
    is_read         tinyint(1)  default 0                 null,
    read_time       datetime                              null,
    is_deleted      tinyint(1)  default 0                 null,
    channel         varchar(20)                           null,
    push_status     varchar(20) default 'PENDING'         null,
    push_time       datetime                              null,
    created_at      datetime    default CURRENT_TIMESTAMP null,
    updated_at      datetime    default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP,
    constraint uk_user_notification
        unique (user_id, notification_id),
    constraint user_notification_ibfk_1
        foreign key (notification_id) references notification (id)
            on delete cascade
)
    charset = utf8mb4;

create index idx_notification
    on user_notification (notification_id);

create index idx_push_status
    on user_notification (push_status);

create index idx_user_read
    on user_notification (user_id, is_read);

