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

create index user_id
    on receipts (user_id);

create index verified_by
    on receipts (verified_by);

