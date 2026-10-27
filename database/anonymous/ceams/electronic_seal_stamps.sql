create table electronic_seal_stamps
(
    id             int auto_increment
        primary key,
    seal_id        int                                not null,
    seal_name      varchar(100)                       not null,
    project_id     int                                not null,
    project_name   varchar(255)                       not null,
    contract_name  varchar(255)                       not null,
    contract_path  varchar(500)                       not null,
    stamp_position varchar(50)                        not null,
    stamp_image    text                               null,
    user_id        int                                null,
    user_name      varchar(100)                       null,
    stamp_time     datetime                           not null,
    ip_address     varchar(50)                        null,
    create_time    datetime default CURRENT_TIMESTAMP not null,
    update_time    datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP,
    constraint fk_stamp_seal
        foreign key (seal_id) references electronic_seals (id)
);

