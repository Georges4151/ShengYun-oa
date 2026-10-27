create table electronic_seal_usages
(
    id            int auto_increment
        primary key,
    seal_id       int                                not null,
    seal_name     varchar(255)                       not null,
    user_id       int                                not null,
    user_name     varchar(255)                       not null,
    purpose       text                               not null,
    document_type varchar(255)                       not null,
    document_path varchar(500)                       not null,
    usage_time    datetime default CURRENT_TIMESTAMP not null,
    ip_address    varchar(50)                        null,
    create_time   datetime default CURRENT_TIMESTAMP not null,
    update_time   datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP,
    constraint fk_usage_seal
        foreign key (seal_id) references electronic_seals (id)
);

create index seal_id
    on electronic_seal_usages (seal_id);

create index usage_time
    on electronic_seal_usages (usage_time);

create index user_id
    on electronic_seal_usages (user_id);

