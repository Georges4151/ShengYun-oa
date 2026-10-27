create table reconciliation_results
(
    id                    int auto_increment
        primary key,
    account_id            int                                   not null,
    statement_id          int                                   null,
    transaction_id        int                                   null,
    status                varchar(20)                           not null comment 'matched, unmatched, partially_matched',
    discrepancy_type      varchar(50)                           null comment 'missing_in_system, missing_in_bank, amount_mismatch, date_mismatch, description_mismatch',
    bank_amount           decimal(18, 2)                        null,
    system_amount         decimal(18, 2)                        null,
    bank_date             date                                  null,
    system_date           date                                  null,
    bank_description      varchar(255)                          null,
    system_description    varchar(255)                          null,
    statement_reference   varchar(100)                          null,
    transaction_reference varchar(100)                          null,
    resolution_status     varchar(20) default 'unresolved'      null comment 'unresolved, resolved',
    resolution_notes      text                                  null,
    reconciliation_date   datetime    default CURRENT_TIMESTAMP null,
    create_time           datetime    default CURRENT_TIMESTAMP null,
    update_time           datetime    default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP
)
    comment '对账结果表' charset = utf8mb4;

create index idx_account_id
    on reconciliation_results (account_id);

create index idx_discrepancy_type
    on reconciliation_results (discrepancy_type);

create index idx_statement_id
    on reconciliation_results (statement_id);

create index idx_status
    on reconciliation_results (status);

create index idx_transaction_id
    on reconciliation_results (transaction_id);

