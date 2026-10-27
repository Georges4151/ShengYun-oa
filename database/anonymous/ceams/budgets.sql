create table budgets
(
    id          int auto_increment
        primary key,
    budget_name varchar(255)                          not null,
    amount      decimal(18, 2)                        not null,
    project_id  int                                   null,
    contract_id int                                   null,
    start_date  date                                  null,
    end_date    date                                  null,
    status      varchar(50) default 'active'          not null,
    created_at  datetime    default CURRENT_TIMESTAMP not null,
    updated_at  datetime    default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP,
    constraint budgets_ibfk_1
        foreign key (project_id) references projects (id),
    constraint budgets_ibfk_2
        foreign key (contract_id) references subcontractor_contracts (id)
);

create index contract_id
    on budgets (contract_id);

create index project_id
    on budgets (project_id);

