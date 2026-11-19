-- ======================================================================
-- V14：BPM 审批内核核心表（M1）
--
-- 说明：
--   1. 八张表统一继承 BaseEntity 审计约定：create_time / update_time /
--      create_by / update_by / deleted / version，主键为雪花 ID（BIGINT）。
--   2. 枚举类字段统一用 VARCHAR 存枚举 name，与现有模块保持一致。
--   3. 索引按《CEAMS V2.0 一期 MVP 迭代开发文档》3.1 要求建立：
--      bpm_task(assignee_id, status, due_at)、bpm_task(instance_id)、
--      bpm_instance(biz_type, biz_id)、bpm_node(instance_id, seq_no)。
--      另补 bpm_task(status, due_at) 供超时扫描分片走索引，避免全表扫描。
--   4. 本迁移只新增表，不修改任何现有业务表。
-- ======================================================================

-- ----------------------------------------------------------------------
-- 1. 流程模板（一种业务对应一个模板）
-- ----------------------------------------------------------------------
CREATE TABLE bpm_template (
    id                 BIGINT       NOT NULL                COMMENT '主键（雪花ID）',
    code               VARCHAR(64)  NOT NULL                COMMENT '模板编码（唯一）',
    name               VARCHAR(128) NOT NULL                COMMENT '模板名称',
    biz_type           VARCHAR(64)  NOT NULL                COMMENT '业务类型（EXPENSE/CHANGE_REQUEST）',
    current_version_id BIGINT       NULL     DEFAULT NULL   COMMENT '当前启用版本ID（逻辑引用 bpm_template_version.id）',
    status             VARCHAR(20)  NOT NULL DEFAULT 'DISABLED' COMMENT '状态：ENABLED/DISABLED',
    create_time        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by          BIGINT       NULL     DEFAULT NULL   COMMENT '创建人ID',
    update_by          BIGINT       NULL     DEFAULT NULL   COMMENT '更新人ID',
    deleted            TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    version            INT          NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
    PRIMARY KEY (id),
    UNIQUE KEY uk_bpm_template_code (code),
    KEY idx_bpm_template_biz_type (biz_type),
    KEY idx_bpm_template_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='BPM流程模板';

-- ----------------------------------------------------------------------
-- 2. 流程模板版本（历史实例锁定创建时版本）
-- ----------------------------------------------------------------------
CREATE TABLE bpm_template_version (
    id             BIGINT       NOT NULL                COMMENT '主键（雪花ID）',
    template_id    BIGINT       NOT NULL                COMMENT '模板ID（逻辑引用 bpm_template.id）',
    version_no     INT          NOT NULL                COMMENT '版本号（同一模板内递增）',
    schema_json    LONGTEXT     NOT NULL                COMMENT '流程定义 JSON（节点、审批人来源、超时时长）',
    status         VARCHAR(20)  NOT NULL DEFAULT 'DRAFT' COMMENT '状态：DRAFT/PUBLISHED/ARCHIVED',
    effective_time DATETIME     NULL     DEFAULT NULL   COMMENT '生效时间',
    create_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by      BIGINT       NULL     DEFAULT NULL   COMMENT '创建人ID',
    update_by      BIGINT       NULL     DEFAULT NULL   COMMENT '更新人ID',
    deleted        TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    version        INT          NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
    PRIMARY KEY (id),
    UNIQUE KEY uk_bpm_template_version_no (template_id, version_no),
    KEY idx_bpm_template_version_status (template_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='BPM流程模板版本';

-- ----------------------------------------------------------------------
-- 3. 流程实例（一次发起一条）
-- ----------------------------------------------------------------------
CREATE TABLE bpm_instance (
    id                  BIGINT       NOT NULL                COMMENT '主键（雪花ID）',
    template_version_id BIGINT       NOT NULL                COMMENT '模板版本ID（逻辑引用 bpm_template_version.id）',
    biz_type            VARCHAR(64)  NOT NULL                COMMENT '业务类型（冗余，便于按业务查询）',
    biz_id              BIGINT       NULL     DEFAULT NULL   COMMENT '业务单据ID（逻辑引用，不建物理外键）',
    initiator_id        BIGINT       NOT NULL                COMMENT '发起人ID（逻辑引用 sys_user.id）',
    status              VARCHAR(20)  NOT NULL DEFAULT 'RUNNING' COMMENT '状态：DRAFT/RUNNING/APPROVED/REJECTED/WITHDRAWN/TERMINATED',
    started_at          DATETIME     NULL     DEFAULT NULL   COMMENT '发起时间',
    ended_at            DATETIME     NULL     DEFAULT NULL   COMMENT '结束时间',
    create_time         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by           BIGINT       NULL     DEFAULT NULL   COMMENT '创建人ID',
    update_by           BIGINT       NULL     DEFAULT NULL   COMMENT '更新人ID',
    deleted             TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    version             INT          NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
    PRIMARY KEY (id),
    KEY idx_bpm_instance_biz (biz_type, biz_id),
    KEY idx_bpm_instance_initiator (initiator_id, status, create_time),
    KEY idx_bpm_instance_template_version (template_version_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='BPM流程实例';

-- ----------------------------------------------------------------------
-- 4. 流程节点实例
-- ----------------------------------------------------------------------
CREATE TABLE bpm_node (
    id           BIGINT       NOT NULL                COMMENT '主键（雪花ID）',
    instance_id  BIGINT       NOT NULL                COMMENT '实例ID（逻辑引用 bpm_instance.id）',
    node_key     VARCHAR(64)  NOT NULL                COMMENT '节点编码（模板内唯一）',
    node_type    VARCHAR(20)  NOT NULL                COMMENT '节点类型：START/SINGLE/COUNTERSIGN/OR_SIGN/CC/CONDITION/AUTO/END',
    seq_no       INT          NOT NULL                COMMENT '节点顺序号（升序流转）',
    status       VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/ACTIVE/COMPLETED/REJECTED/CLOSED',
    arrived_at   DATETIME     NULL     DEFAULT NULL   COMMENT '到达时间',
    completed_at DATETIME     NULL     DEFAULT NULL   COMMENT '完成时间',
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by    BIGINT       NULL     DEFAULT NULL   COMMENT '创建人ID',
    update_by    BIGINT       NULL     DEFAULT NULL   COMMENT '更新人ID',
    deleted      TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    version      INT          NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
    PRIMARY KEY (id),
    UNIQUE KEY uk_bpm_node_instance_key (instance_id, node_key),
    KEY idx_bpm_node_instance_seq (instance_id, seq_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='BPM流程节点实例';

-- ----------------------------------------------------------------------
-- 5. 审批任务（全系统唯一待办来源）
-- ----------------------------------------------------------------------
CREATE TABLE bpm_task (
    id          BIGINT       NOT NULL                COMMENT '主键（雪花ID）',
    instance_id BIGINT       NOT NULL                COMMENT '实例ID（逻辑引用 bpm_instance.id）',
    node_id     BIGINT       NOT NULL                COMMENT '节点ID（逻辑引用 bpm_node.id）',
    assignee_id BIGINT       NULL     DEFAULT NULL   COMMENT '处理人ID（逻辑引用 sys_user.id；解析为空时置空）',
    source_type VARCHAR(30)  NOT NULL                COMMENT '审批人来源：USER/ROLE/DEPT_LEADER/SUPERIOR/INITIATOR/DELEGATE',
    task_type   VARCHAR(20)  NOT NULL DEFAULT 'APPROVE' COMMENT '任务类型：APPROVE/CC',
    status      VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/PENDING_ASSIGNEE/APPROVED/REJECTED/CLOSED',
    due_at      DATETIME     NULL     DEFAULT NULL   COMMENT '超时时间（超过后仅提醒，不自动流转）',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by   BIGINT       NULL     DEFAULT NULL   COMMENT '创建人ID',
    update_by   BIGINT       NULL     DEFAULT NULL   COMMENT '更新人ID',
    deleted     TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    version     INT          NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
    PRIMARY KEY (id),
    KEY idx_bpm_task_assignee_status_due (assignee_id, status, due_at),
    KEY idx_bpm_task_instance (instance_id),
    KEY idx_bpm_task_node (node_id),
    KEY idx_bpm_task_status_due (status, due_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='BPM审批任务（统一待办）';

-- ----------------------------------------------------------------------
-- 6. 审批人快照（固化审批人，防止人员变动导致待办漂移）
-- ----------------------------------------------------------------------
CREATE TABLE bpm_assignee_snapshot (
    id          BIGINT       NOT NULL                COMMENT '主键（雪花ID）',
    node_id     BIGINT       NOT NULL                COMMENT '节点ID（逻辑引用 bpm_node.id）',
    assignee_id BIGINT       NOT NULL                COMMENT '审批人ID（逻辑引用 sys_user.id）',
    source_type VARCHAR(30)  NOT NULL                COMMENT '解析来源：USER/ROLE/DEPT_LEADER/SUPERIOR/INITIATOR/DELEGATE',
    resolved_at DATETIME     NULL     DEFAULT NULL   COMMENT '解析时间',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by   BIGINT       NULL     DEFAULT NULL   COMMENT '创建人ID',
    update_by   BIGINT       NULL     DEFAULT NULL   COMMENT '更新人ID',
    deleted     TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    version     INT          NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
    PRIMARY KEY (id),
    KEY idx_bpm_assignee_snapshot_node (node_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='BPM审批人快照';

-- ----------------------------------------------------------------------
-- 7. 抄送记录
-- ----------------------------------------------------------------------
CREATE TABLE bpm_cc_record (
    id          BIGINT       NOT NULL                COMMENT '主键（雪花ID）',
    instance_id BIGINT       NOT NULL                COMMENT '实例ID（逻辑引用 bpm_instance.id）',
    node_id     BIGINT       NOT NULL                COMMENT '节点ID（逻辑引用 bpm_node.id）',
    receiver_id BIGINT       NOT NULL                COMMENT '抄送接收人ID（逻辑引用 sys_user.id）',
    read_status VARCHAR(20)  NOT NULL DEFAULT 'UNREAD' COMMENT '阅读状态：UNREAD/READ',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by   BIGINT       NULL     DEFAULT NULL   COMMENT '创建人ID',
    update_by   BIGINT       NULL     DEFAULT NULL   COMMENT '更新人ID',
    deleted     TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    version     INT          NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
    PRIMARY KEY (id),
    KEY idx_bpm_cc_receiver (receiver_id, read_status, create_time),
    KEY idx_bpm_cc_instance (instance_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='BPM抄送记录';

-- ----------------------------------------------------------------------
-- 8. 流程变量（业务ID与条件变量）
-- ----------------------------------------------------------------------
CREATE TABLE bpm_variable (
    id          BIGINT        NOT NULL                COMMENT '主键（雪花ID）',
    instance_id BIGINT        NOT NULL                COMMENT '实例ID（逻辑引用 bpm_instance.id）',
    var_key     VARCHAR(100)  NOT NULL                COMMENT '变量名',
    var_value   VARCHAR(1000) NULL     DEFAULT NULL   COMMENT '变量值（统一字符串存储）',
    create_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by   BIGINT        NULL     DEFAULT NULL   COMMENT '创建人ID',
    update_by   BIGINT        NULL     DEFAULT NULL   COMMENT '更新人ID',
    deleted     TINYINT       NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    version     INT           NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
    PRIMARY KEY (id),
    UNIQUE KEY uk_bpm_variable_instance_key (instance_id, var_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='BPM流程变量';
