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

