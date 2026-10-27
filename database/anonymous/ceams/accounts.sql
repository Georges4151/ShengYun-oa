create table accounts
(
    account_id         int auto_increment
        primary key,
    project_id         int                                   null,
    transaction_date   date                                  not null,
    amount             decimal(12, 2)                        not null,
    type               varchar(50)                           not null,
    receipt_id         int                                   null,
    description        text                                  null,
    status             varchar(50) default 'draft'           not null,
    created_at         datetime    default CURRENT_TIMESTAMP not null,
    updated_at         datetime    default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP,
    payment_account_id int                                   null,
    contract_id        int                                   null comment '合同ID',
    payment_type       varchar(50)                           null comment '支付类型',
    allocation_status  varchar(20)                           null comment '成本分摊状态',
    constraint accounts_ibfk_1
        foreign key (project_id) references projects (id),
    constraint accounts_ibfk_2
        foreign key (receipt_id) references receipts (id)
);

create index project_id
    on accounts (project_id);

create index receipt_id
    on accounts (receipt_id);

