create table payment_account
(
    id          int auto_increment
        primary key,
    name        varchar(100)                             not null,
    type        varchar(50)                              not null,
    balance     decimal(18, 2) default 0.00              null,
    description text                                     null,
    create_time datetime       default CURRENT_TIMESTAMP null,
    update_time datetime       default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP
)
    charset = utf8mb4;

