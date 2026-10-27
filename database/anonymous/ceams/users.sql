create table users
(
    user_id       int auto_increment
        primary key,
    username      varchar(50)                           not null,
    password_hash varchar(255)                          not null,
    role          varchar(20) default 'EMPLOYEE'        not null comment '用户角色',
    name          varchar(100)                          not null,
    email         varchar(100)                          null,
    created_at    datetime    default CURRENT_TIMESTAMP not null,
    updated_at    datetime    default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP,
    constraint email
        unique (email),
    constraint username
        unique (username)
);

