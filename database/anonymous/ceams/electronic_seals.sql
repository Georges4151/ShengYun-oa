create table electronic_seals
(
    id                 int auto_increment
        primary key,
    seal_name          varchar(255)                          not null,
    seal_type          varchar(50)                           not null,
    description        text                                  null,
    status             varchar(50) default 'inactive'        not null,
    create_time        datetime    default CURRENT_TIMESTAMP not null,
    update_time        datetime    default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP,
    seal_image         text                                  null,
    storage_type       varchar(20) default 'local'           null comment '存储类型',
    seal_file_id       varchar(50)                           null comment '电子章文件ID',
    usb_drive_id       varchar(100)                          null comment 'U盘唯一标识',
    password_protected tinyint(1)  default 0                 null comment '是否密码保护',
    last_used_time     datetime                              null comment '最后使用时间',
    usage_count        int         default 0                 null comment '使用次数'
);

