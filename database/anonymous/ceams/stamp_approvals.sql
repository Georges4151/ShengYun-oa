create table stamp_approvals
(
    id                     int auto_increment
        primary key,
    stamp_id               int                                   null,
    contract_name          varchar(255)                          not null,
    contract_path          varchar(500)                          null,
    original_contract_path varchar(500)                          null comment '原始合同文件路径',
    stamp_positions        longtext                              null comment '印章位置信息JSON',
    stamped_file_path      varchar(500)                          null comment 'Stamped file path',
    seal_id                int                                   not null,
    seal_name              varchar(100)                          not null,
    seal_code              varchar(50)                           null,
    seal_permission        varchar(500)                          null,
    project_id             int                                   null,
    project_name           varchar(200)                          null,
    applicant_id           int                                   not null,
    applicant_name         varchar(100)                          not null,
    approver_id            int                                   null,
    approver_name          varchar(100)                          null,
    status                 varchar(20) default 'pending'         null,
    reject_reason          text                                  null,
    apply_time             datetime    default CURRENT_TIMESTAMP null,
    approval_time          datetime                              null,
    create_time            datetime    default CURRENT_TIMESTAMP null,
    update_time            datetime    default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP
)
    charset = utf8mb4;

create index idx_applicant_id
    on stamp_approvals (applicant_id);

create index idx_apply_time
    on stamp_approvals (apply_time);

create index idx_project_id
    on stamp_approvals (project_id);

create index idx_seal_id
    on stamp_approvals (seal_id);

create index idx_status
    on stamp_approvals (status);

