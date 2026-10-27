create table receipt_verification
(
    id                 int auto_increment
        primary key,
    receipt_id         int                                not null,
    payment_account_id int                                not null,
    amount             decimal(10, 2)                     not null,
    payment_method     varchar(50)                        not null,
    verification_time  datetime                           not null,
    verified_by        int                                not null,
    remark             varchar(255)                       null,
    create_time        datetime default CURRENT_TIMESTAMP null,
    update_time        datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP,
    constraint receipt_verification_ibfk_1
        foreign key (receipt_id) references receipts (id)
            on delete cascade,
    constraint receipt_verification_ibfk_2
        foreign key (payment_account_id) references payment_account (id)
            on delete cascade
);

create index payment_account_id
    on receipt_verification (payment_account_id);

create index receipt_id
    on receipt_verification (receipt_id);

