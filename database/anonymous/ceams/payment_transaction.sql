create table payment_transaction
(
    id               int auto_increment
        primary key,
    account_id       int                                not null,
    receipt_id       int                                null,
    amount           decimal(18, 2)                     not null,
    type             varchar(20)                        not null,
    transaction_date datetime                           not null,
    description      text                               null,
    create_time      datetime default CURRENT_TIMESTAMP null,
    constraint payment_transaction_ibfk_1
        foreign key (account_id) references payment_account (id)
            on delete cascade
)
    charset = utf8mb4;

create index idx_payment_transaction_account_id
    on payment_transaction (account_id);

create index idx_payment_transaction_receipt_id
    on payment_transaction (receipt_id);

create index idx_payment_transaction_transaction_date
    on payment_transaction (transaction_date);

create index idx_payment_transaction_type
    on payment_transaction (type);

