create table projects
(
    id              int auto_increment
        primary key,
    project_name    varchar(200)                             not null,
    client_name     varchar(200)                             not null,
    project_address varchar(500)                             not null,
    project_type    varchar(100)                             not null,
    start_date      date                                     not null,
    end_date        date                                     null,
    contract_amount decimal(18, 2)                           not null,
    budget_amount   decimal(18, 2)                           not null,
    actual_amount   decimal(18, 2) default 0.00              null,
    status          varchar(50)                              not null,
    description     text                                     null,
    create_time     datetime       default CURRENT_TIMESTAMP not null,
    update_time     datetime       default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP,
    manager_id      int                                      null comment '项目经理ID',
    manager_name    varchar(100)                             null comment '项目经理姓名',
    constraint fk_project_manager
        foreign key (manager_id) references users (user_id)
            on update cascade on delete set null
);

create index idx_project_manager_id
    on projects (manager_id);

