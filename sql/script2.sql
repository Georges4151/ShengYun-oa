create table approval_record
(
    id             int auto_increment
        primary key,
    receipt_id     int                                not null,
    approval_level int                                not null comment '审核级别: 1-财务审核, 2-项目经理审核, 3-经理审核',
    approver_role  varchar(50)                        null comment '审核人角色',
    approver_id    int                                null comment '审核人ID',
    approver_name  varchar(100)                       null comment '审核人姓名',
    status         varchar(20)                        null comment '审核状态: pending-待审核, approved-审核通过, rejected-审核拒绝',
    comment        text                               null comment '审核意见',
    approval_time  datetime                           null comment '审核时间',
    create_time    datetime default CURRENT_TIMESTAMP null,
    update_time    datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP
)
    comment '审核记录表' charset = utf8mb4;

create index idx_approval_level
    on approval_record (approval_level);

create index idx_receipt_id
    on approval_record (receipt_id);

create index idx_status
    on approval_record (status);

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

create table electronic_seals
(
    id                 int auto_increment
        primary key,
    seal_name          varchar(255)                          not null,
    seal_type          varchar(50)                           not null,
    description        text                                  null,
    status             varchar(50) default 'inactive'        not null,
    create_time        datetime    default CURRENT_TIMESTAMP not null,
    update_time        datetime    default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP,
    seal_image         text                                  null,
    storage_type       varchar(20) default 'local'           null comment '存储类型',
    seal_file_id       varchar(50)                           null comment '电子章文件ID',
    usb_drive_id       varchar(100)                          null comment 'U盘唯一标识',
    password_protected tinyint(1)  default 0                 null comment '是否密码保护',
    last_used_time     datetime                              null comment '最后使用时间',
    usage_count        int         default 0                 null comment '使用次数'
);

create table electronic_seal_applications
(
    id               int auto_increment
        primary key,
    seal_id          int                                   not null,
    seal_name        varchar(255)                          not null,
    applicant_id     int                                   not null,
    applicant_name   varchar(255)                          not null,
    purpose          text                                  not null,
    status           varchar(50) default 'pending'         not null,
    approver_id      int                                   null,
    approver_name    varchar(255)                          null,
    approval_comment text                                  null,
    apply_time       datetime    default CURRENT_TIMESTAMP not null,
    approval_time    datetime                              null,
    create_time      datetime    default CURRENT_TIMESTAMP not null,
    update_time      datetime    default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP,
    constraint fk_application_seal
        foreign key (seal_id) references electronic_seals (id)
);

create index applicant_id
    on electronic_seal_applications (applicant_id);

create index seal_id
    on electronic_seal_applications (seal_id);

create index status
    on electronic_seal_applications (status);

create table electronic_seal_stamps
(
    id             int auto_increment
        primary key,
    seal_id        int                                not null,
    seal_name      varchar(100)                       not null,
    project_id     int                                not null,
    project_name   varchar(255)                       not null,
    contract_name  varchar(255)                       not null,
    contract_path  varchar(500)                       not null,
    stamp_position varchar(50)                        not null,
    stamp_image    text                               null,
    user_id        int                                null,
    user_name      varchar(100)                       null,
    stamp_time     datetime                           not null,
    ip_address     varchar(50)                        null,
    create_time    datetime default CURRENT_TIMESTAMP not null,
    update_time    datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP,
    constraint fk_stamp_seal
        foreign key (seal_id) references electronic_seals (id)
);

create table electronic_seal_usages
(
    id            int auto_increment
        primary key,
    seal_id       int                                not null,
    seal_name     varchar(255)                       not null,
    user_id       int                                not null,
    user_name     varchar(255)                       not null,
    purpose       text                               not null,
    document_type varchar(255)                       not null,
    document_path varchar(500)                       not null,
    usage_time    datetime default CURRENT_TIMESTAMP not null,
    ip_address    varchar(50)                        null,
    create_time   datetime default CURRENT_TIMESTAMP not null,
    update_time   datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP,
    constraint fk_usage_seal
        foreign key (seal_id) references electronic_seals (id)
);

create index seal_id
    on electronic_seal_usages (seal_id);

create index usage_time
    on electronic_seal_usages (usage_time);

create index user_id
    on electronic_seal_usages (user_id);

create table materials
(
    material_id   int auto_increment
        primary key,
    material_name varchar(200)                       not null,
    material_code varchar(50)                        not null,
    specification varchar(200)                       not null,
    unit          varchar(20)                        not null,
    current_price decimal(12, 2)                     not null,
    supplier_id   int                                null,
    category_id   int                                null,
    created_at    datetime default CURRENT_TIMESTAMP not null,
    updated_at    datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP,
    constraint material_code
        unique (material_code)
);

create table notification
(
    id                int auto_increment
        primary key,
    title             varchar(200)                          not null,
    content_type      varchar(20)                           not null,
    content           text                                  not null,
    link_url          varchar(500)                          null,
    priority          varchar(10) default 'MEDIUM'          not null,
    notification_type varchar(50)                           not null,
    sender_id         int                                   null,
    sender_name       varchar(100)                          null,
    valid_start_time  datetime                              null,
    valid_end_time    datetime                              null,
    status            varchar(20) default 'DRAFT'           not null,
    publish_time      datetime                              null,
    created_at        datetime    default CURRENT_TIMESTAMP null,
    updated_at        datetime    default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP
)
    charset = utf8mb4;

create index idx_priority
    on notification (priority);

create index idx_publish_time
    on notification (publish_time);

create index idx_status
    on notification (status);

create index idx_type
    on notification (notification_type);

create table payment_account
(
    id          int auto_increment
        primary key,
    name        varchar(100)                             not null,
    type        varchar(50)                              not null,
    balance     decimal(18, 2) default 0.00              null,
    description text                                     null,
    create_time datetime       default CURRENT_TIMESTAMP null,
    update_time datetime       default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP
)
    charset = utf8mb4;

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

create table user_notification
(
    id              int auto_increment
        primary key,
    notification_id int                                   not null,
    user_id         int                                   not null,
    is_read         tinyint(1)  default 0                 null,
    read_time       datetime                              null,
    is_deleted      tinyint(1)  default 0                 null,
    channel         varchar(20)                           null,
    push_status     varchar(20) default 'PENDING'         null,
    push_time       datetime                              null,
    created_at      datetime    default CURRENT_TIMESTAMP null,
    updated_at      datetime    default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP,
    constraint uk_user_notification
        unique (user_id, notification_id),
    constraint user_notification_ibfk_1
        foreign key (notification_id) references notification (id)
            on delete cascade
)
    charset = utf8mb4;

create index idx_notification
    on user_notification (notification_id);

create index idx_push_status
    on user_notification (push_status);

create index idx_user_read
    on user_notification (user_id, is_read);

create table user_notification_preference
(
    id                 int auto_increment
        primary key,
    user_id            int                                   not null,
    notification_type  varchar(50)                           not null,
    receive_system     tinyint(1)  default 1                 null,
    receive_email      tinyint(1)  default 0                 null,
    receive_sms        tinyint(1)  default 0                 null,
    quiet_mode         tinyint(1)  default 0                 null,
    quiet_start_time   time                                  null,
    quiet_end_time     time                                  null,
    priority_threshold varchar(10) default 'LOW'             null,
    created_at         datetime    default CURRENT_TIMESTAMP null,
    updated_at         datetime    default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP,
    constraint uk_user_type
        unique (user_id, notification_type)
)
    charset = utf8mb4;

create table users
(
    user_id       int auto_increment
        primary key,
    username      varchar(50)                           not null,
    password_hash varchar(255)                          not null,
    role          varchar(20) default 'EMPLOYEE'        not null comment '用户角色',
    name          varchar(100)                          not null,
    email         varchar(100)                          null,
    created_at    datetime    default CURRENT_TIMESTAMP not null,
    updated_at    datetime    default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP,
    constraint email
        unique (email),
    constraint username
        unique (username)
);

create table operation_logs
(
    log_id                int auto_increment
        primary key,
    user_id               int                                not null,
    operation_type        varchar(50)                        not null,
    operation_description text                               not null,
    target_id             int                                null,
    target_type           varchar(50)                        null,
    ip_address            varchar(50)                        null,
    created_at            datetime default CURRENT_TIMESTAMP not null,
    constraint operation_logs_ibfk_1
        foreign key (user_id) references users (user_id)
);

create index user_id
    on operation_logs (user_id);

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

create table receipts
(
    id                          int auto_increment
        primary key,
    user_id                     int                                      not null,
    receipt_date                date                                     not null,
    amount                      decimal(12, 2)                           not null,
    type                        varchar(50)                              not null,
    description                 text                                     null comment '票据描述',
    file_path                   varchar(255)                             not null,
    file_hash                   varchar(255)                             not null,
    ocr_data                    text                                     null,
    verification_status         varchar(50)    default 'pending'         not null,
    verified_by                 int                                      null,
    verified_at                 datetime                                 null,
    create_time                 datetime       default CURRENT_TIMESTAMP not null,
    update_time                 datetime       default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP,
    reporter_id                 int                                      null comment '报票人ID',
    project_id                  int                                      null comment '归属项目ID',
    reporter_name               varchar(100)                             null,
    payment_status              varchar(50)    default 'unpaid'          not null comment '支付状态: unpaid(未支付), paid(已支付), partially_paid(部分支付)',
    paid_amount                 decimal(12, 2) default 0.00              null comment '已支付金额',
    paid_at                     datetime                                 null comment '支付时间',
    paid_by                     int                                      null comment '支付人ID',
    payment_method              varchar(50)                              null comment '鏀?粯鏂瑰紡: cash(鐜伴噾), bank_transfer(閾惰?杞?处), wechat(寰?俊), alipay(鏀?粯瀹?, other(鍏朵粬)',
    finance_status              varchar(20)    default 'pending'         null comment '财务审核状态: pending-待审核, approved-审核通过, rejected-审核拒绝',
    finance_verified_by         int                                      null comment '财务审核人ID',
    finance_verified_at         datetime                                 null comment '财务审核时间',
    finance_comment             text                                     null comment '财务审核意见',
    project_manager_status      varchar(20)    default 'pending'         null comment '项目经理审核状态',
    project_manager_verified_by int                                      null comment '项目经理审核人ID',
    project_manager_verified_at datetime                                 null comment '项目经理审核时间',
    project_manager_comment     text                                     null comment '项目经理审核意见',
    manager_status              varchar(20)    default 'pending'         null comment '经理审核状态',
    manager_verified_by         int                                      null comment '经理审核人ID',
    manager_verified_at         datetime                                 null comment '经理审核时间',
    manager_comment             text                                     null comment '经理审核意见',
    current_approval_level      int            default 0                 null comment '当前审核级别: 0-待财务审核, 1-待项目经理审核, 2-待经理审核, 3-审核完成',
    receipt_number              varchar(100)                             null comment '票据编号',
    constraint uk_receipt_number
        unique (receipt_number),
    constraint fk_receipts_paid_by
        foreign key (paid_by) references users (user_id),
    constraint receipts_ibfk_1
        foreign key (user_id) references users (user_id),
    constraint receipts_ibfk_2
        foreign key (verified_by) references users (user_id)
);

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

create index user_id
    on receipts (user_id);

create index verified_by
    on receipts (verified_by);

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

create table settlement_proofs
(
    id                int auto_increment comment '主键ID'
        primary key,
    contract_id       int                                not null comment '关联合同ID',
    subcontractor_id  int                                not null comment '分包商ID',
    proof_name        varchar(200)                       not null comment '结清证明名称',
    proof_number      varchar(100)                       null comment '结清证明编号',
    proof_date        date                               null comment '结清证明日期',
    settlement_amount decimal(15, 2)                     null comment '结清金额',
    file_path         varchar(500)                       null comment '文件存储路径',
    file_name         varchar(200)                       null comment '原始文件名',
    description       text                               null comment '备注说明',
    create_time       datetime default CURRENT_TIMESTAMP null comment '创建时间',
    update_time       datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    created_by        int                                null comment '创建人ID',
    constraint settlement_proofs_ibfk_1
        foreign key (contract_id) references subcontractor_contracts (id)
            on delete cascade,
    constraint settlement_proofs_ibfk_2
        foreign key (subcontractor_id) references subcontractors (id)
            on delete cascade
)
    comment '结清证明合同表' charset = utf8mb4;

create index idx_contract_id
    on settlement_proofs (contract_id);

create index idx_subcontractor_id
    on settlement_proofs (subcontractor_id);

create index idx_subcontractor_contracts_project
    on subcontractor_contracts (project_id);

create index idx_subcontractor_contracts_status
    on subcontractor_contracts (status);

create index idx_subcontractor_contracts_subcontractor
    on subcontractor_contracts (subcontractor_id);

create table subcontractor_project
(
    id               int auto_increment
        primary key,
    subcontractor_id int                                not null,
    project_id       int                                not null,
    create_time      datetime default CURRENT_TIMESTAMP null,
    update_time      datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP,
    constraint uk_subcontractor_project
        unique (subcontractor_id, project_id),
    constraint subcontractor_project_ibfk_1
        foreign key (subcontractor_id) references subcontractors (id)
            on delete cascade,
    constraint subcontractor_project_ibfk_2
        foreign key (project_id) references projects (id)
            on delete cascade
);

create index idx_subcontractor_project_project
    on subcontractor_project (project_id);

create index idx_subcontractor_project_subcontractor
    on subcontractor_project (subcontractor_id);


