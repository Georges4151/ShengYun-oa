create table subcontractor_contracts
(
    id               int auto_increment
        primary key,
    subcontractor_id int                                      not null,
    project_id       int                                      null,
    contract_name    varchar(255)                             not null,
    contract_number  varchar(255)                             not null,
    contract_amount  decimal(18, 2)                           not null,
    start_date       date                                     null,
    end_date         date                                     null,
    status           varchar(50)    default 'ACTIVE'          null,
    description      text                                     null,
    create_time      datetime       default CURRENT_TIMESTAMP null,
    update_time      datetime       default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP,
    file_path        varchar(255)                             null,
    file_name        varchar(255)                             null,
    paid_amount      decimal(18, 2) default 0.00              null comment '已支付金额',
    constraint subcontractor_contracts_ibfk_1
        foreign key (subcontractor_id) references subcontractors (id)
            on delete cascade,
    constraint subcontractor_contracts_ibfk_2
        foreign key (project_id) references projects (id)
            on delete set null
);

create index idx_subcontractor_contracts_project
    on subcontractor_contracts (project_id);

create index idx_subcontractor_contracts_status
    on subcontractor_contracts (status);

create index idx_subcontractor_contracts_subcontractor
    on subcontractor_contracts (subcontractor_id);

