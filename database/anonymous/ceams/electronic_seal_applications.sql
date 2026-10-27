create table electronic_seal_applications
(
    id               int auto_increment
        primary key,
    seal_id          int                                   not null,
    seal_name        varchar(255)                          not null,
    applicant_id     int                                   not null,
    applicant_name   varchar(255)                          not null,
    purpose          text                                  not null,
    status           varchar(50) default 'pending'         not null,
    approver_id      int                                   null,
    approver_name    varchar(255)                          null,
    approval_comment text                                  null,
    apply_time       datetime    default CURRENT_TIMESTAMP not null,
    approval_time    datetime                              null,
    create_time      datetime    default CURRENT_TIMESTAMP not null,
    update_time      datetime    default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP,
    constraint fk_application_seal
        foreign key (seal_id) references electronic_seals (id)
);

create index applicant_id
    on electronic_seal_applications (applicant_id);

create index seal_id
    on electronic_seal_applications (seal_id);

create index status
    on electronic_seal_applications (status);

