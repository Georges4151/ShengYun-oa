create table subcontractors
(
    id                        int auto_increment
        primary key,
    name                      varchar(200)                       not null,
    contact_person            varchar(50)                        not null,
    phone                     varchar(20)                        not null,
    address                   text                               null,
    qualification_level       varchar(50)                        null,
    credit_rating             varchar(20)                        null,
    business_scope            text                               null,
    tax_identification_number varchar(50)                        null,
    bank_account              varchar(100)                       null,
    description               text                               null,
    create_time               datetime default CURRENT_TIMESTAMP not null,
    update_time               datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP
);

