create table notification
(
    id                int auto_increment
        primary key,
    title             varchar(200)                          not null,
    content_type      varchar(20)                           not null,
    content           text                                  not null,
    link_url          varchar(500)                          null,
    priority          varchar(10) default 'MEDIUM'          not null,
    notification_type varchar(50)                           not null,
    sender_id         int                                   null,
    sender_name       varchar(100)                          null,
    valid_start_time  datetime                              null,
    valid_end_time    datetime                              null,
    status            varchar(20) default 'DRAFT'           not null,
    publish_time      datetime                              null,
    created_at        datetime    default CURRENT_TIMESTAMP null,
    updated_at        datetime    default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP
)
    charset = utf8mb4;

create index idx_priority
    on notification (priority);

create index idx_publish_time
    on notification (publish_time);

create index idx_status
    on notification (status);

create index idx_type
    on notification (notification_type);

