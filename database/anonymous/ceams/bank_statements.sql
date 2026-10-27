create table bank_statements
(
    id               int auto_increment
        primary key,
    account_id       int                                   not null,
    transaction_date date                                  not null,
    amount           decimal(18, 2)                        not null,
    type             varchar(20)                           not null comment 'income or expense',
    description      varchar(255)                          null,
    counterparty     varchar(100)                          null,
    bank_reference   varchar(100)                          null,
    balance          decimal(18, 2)                        null,
    status           varchar(20) default 'unreconciled'    null comment 'unreconciled, reconciled, matched',
    create_time      datetime    default CURRENT_TIMESTAMP null,
    update_time      datetime    default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP
)
    comment '银行流水表' charset = utf8mb4;

create index idx_account_id
    on bank_statements (account_id);

create index idx_status
    on bank_statements (status);

create index idx_transaction_date
    on bank_statements (transaction_date);

