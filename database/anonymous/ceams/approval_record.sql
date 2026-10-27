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

