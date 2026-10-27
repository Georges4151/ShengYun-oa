create table operation_logs
(
    log_id                int auto_increment
        primary key,
    user_id               int                                not null,
    operation_type        varchar(50)                        not null,
    operation_description text                               not null,
    target_id             int                                null,
    target_type           varchar(50)                        null,
    ip_address            varchar(50)                        null,
    created_at            datetime default CURRENT_TIMESTAMP not null,
    constraint operation_logs_ibfk_1
        foreign key (user_id) references users (user_id)
);

create index user_id
    on operation_logs (user_id);

