-- ======================================================================
-- CEAMS 全量建库脚本（V1）
-- 说明：
--   1. 表前缀：sys_（iam 域）/ biz_（其余业务域）。
--   2. 所有业务表含 BaseEntity 审计字段：id(BIGINT 雪花, 非 AUTO_INCREMENT)、
--      create_time/update_time(DATETIME)、create_by/update_by(BIGINT 逻辑引用)、
--      deleted(TINYINT 默认0)、version(INT 默认0)。
--   3. 金额 DECIMAL(18,2)；时间 DATETIME；枚举 VARCHAR + 代码值（不落中文）。
--   4. 敏感字段（税号/银行账号/手机号）VARCHAR，COMMENT 标注【需加密】。
--   5. 物理外键仅保留核心 1:1；本库统一采用逻辑引用 + 普通索引，不建 FOREIGN KEY，
--      以避免建表顺序与逻辑删除带来的级联风险。
--   6. 统一 utf8mb4。雪花 ID 由应用（MyBatis-Plus ASSIGN_ID）生成。
-- ======================================================================

-- ======================================================================
-- 一、iam 域（sys_ 前缀）
-- ======================================================================

-- 1. 系统用户
CREATE TABLE sys_user (
    id              BIGINT       NOT NULL                COMMENT '主键（雪花ID）',
    username        VARCHAR(50)  NOT NULL                COMMENT '登录账号',
    password_hash   VARCHAR(255) NOT NULL                COMMENT '密码（BCrypt）',
    name            VARCHAR(100) NOT NULL                COMMENT '真实姓名',
    email           VARCHAR(100) NULL     DEFAULT NULL   COMMENT '邮箱',
    phone           VARCHAR(20)  NULL     DEFAULT NULL   COMMENT '手机号【需加密】',
    status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE/DISABLED/LOCKED',
    last_login_time DATETIME     NULL     DEFAULT NULL   COMMENT '最后登录时间',
    last_login_ip   VARCHAR(50)  NULL     DEFAULT NULL   COMMENT '最后登录IP',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by       BIGINT       NULL     DEFAULT NULL   COMMENT '创建人ID',
    update_by       BIGINT       NULL     DEFAULT NULL   COMMENT '更新人ID',
    deleted         TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    version         INT          NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_user_username (username),
    KEY idx_sys_user_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统用户';

-- 2. 角色
CREATE TABLE sys_role (
    id          BIGINT       NOT NULL                COMMENT '主键（雪花ID）',
    code        VARCHAR(50)  NOT NULL                COMMENT '角色编码（如 SYS_ADMIN）',
    name        VARCHAR(100) NOT NULL                COMMENT '角色名',
    description VARCHAR(255) NULL     DEFAULT NULL   COMMENT '描述',
    status      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE/DISABLED',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by   BIGINT       NULL     DEFAULT NULL   COMMENT '创建人ID',
    update_by   BIGINT       NULL     DEFAULT NULL   COMMENT '更新人ID',
    deleted     TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    version     INT          NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_role_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色';

-- 3. 权限（菜单/按钮/API）
CREATE TABLE sys_permission (
    id         BIGINT       NOT NULL                COMMENT '主键（雪花ID）',
    code       VARCHAR(100) NOT NULL                COMMENT '权限标识（如 project:create）',
    name       VARCHAR(100) NOT NULL                COMMENT '权限名',
    type       VARCHAR(20)  NOT NULL                COMMENT '类型：MENU/BUTTON/API',
    parent_id  BIGINT       NULL     DEFAULT NULL   COMMENT '父权限ID（逻辑引用）',
    path       VARCHAR(255) NULL     DEFAULT NULL   COMMENT '前端路由（MENU）',
    icon       VARCHAR(100) NULL     DEFAULT NULL   COMMENT '图标',
    sort_order INT          NOT NULL DEFAULT 0      COMMENT '排序',
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by  BIGINT       NULL     DEFAULT NULL   COMMENT '创建人ID',
    update_by  BIGINT       NULL     DEFAULT NULL   COMMENT '更新人ID',
    deleted    TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    version    INT          NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_permission_code (code),
    KEY idx_sys_permission_parent (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='权限（菜单/按钮/API）';

-- 4. 用户-角色 关联（RBAC；物理删除，无逻辑删除）
CREATE TABLE sys_user_role (
    id          BIGINT      NOT NULL COMMENT '主键（雪花ID）',
    user_id     BIGINT      NOT NULL COMMENT '用户ID（逻辑引用 sys_user.id）',
    role_id     BIGINT      NOT NULL COMMENT '角色ID（逻辑引用 sys_role.id）',
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    create_by   BIGINT      NULL     DEFAULT NULL COMMENT '创建人ID',
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_user_role (user_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户-角色关联';

-- 5. 角色-权限 关联（RBAC；物理删除，无逻辑删除）
CREATE TABLE sys_role_permission (
    id            BIGINT   NOT NULL COMMENT '主键（雪花ID）',
    role_id       BIGINT   NOT NULL COMMENT '角色ID（逻辑引用 sys_role.id）',
    permission_id BIGINT   NOT NULL COMMENT '权限ID（逻辑引用 sys_permission.id）',
    create_time   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    create_by     BIGINT   NULL     DEFAULT NULL COMMENT '创建人ID',
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_role_permission (role_id, permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色-权限关联';

-- 6. 用户设备绑定
CREATE TABLE sys_user_device (
    id              BIGINT       NOT NULL                COMMENT '主键（雪花ID）',
    user_id         BIGINT       NOT NULL                COMMENT '用户ID（逻辑引用 sys_user.id）',
    device_id       VARCHAR(128) NOT NULL                COMMENT '设备指纹',
    device_name     VARCHAR(128) NULL     DEFAULT NULL   COMMENT '设备名',
    platform        VARCHAR(20)  NULL     DEFAULT NULL   COMMENT '平台：WEB/DESKTOP/MINIPROGRAM',
    last_login_ip   VARCHAR(50)  NULL     DEFAULT NULL   COMMENT '最后登录IP',
    status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE/UNBOUND',
    bind_time       DATETIME     NULL     DEFAULT NULL   COMMENT '绑定时间',
    last_login_time DATETIME     NULL     DEFAULT NULL   COMMENT '最后登录时间',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by       BIGINT       NULL     DEFAULT NULL   COMMENT '创建人ID',
    update_by       BIGINT       NULL     DEFAULT NULL   COMMENT '更新人ID',
    deleted         TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    version         INT          NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
    PRIMARY KEY (id),
    KEY idx_sys_user_device_user (user_id),
    KEY idx_sys_user_device_device (device_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户设备绑定';

-- 7. RefreshToken（刷新令牌轮换与吊销）
CREATE TABLE sys_refresh_token (
    id              BIGINT       NOT NULL                COMMENT '主键（雪花ID）',
    user_id         BIGINT       NOT NULL                COMMENT '用户ID（逻辑引用 sys_user.id）',
    jti             VARCHAR(64)  NOT NULL                COMMENT '令牌唯一标识',
    token_hash      VARCHAR(255) NOT NULL                COMMENT 'refresh token 哈希',
    device_id       VARCHAR(128) NULL     DEFAULT NULL   COMMENT '设备指纹',
    expires_at      DATETIME     NOT NULL                COMMENT '过期时间',
    revoked         TINYINT      NOT NULL DEFAULT 0      COMMENT '是否已吊销：0有效 1已吊销',
    replaced_by_jti VARCHAR(64)  NULL     DEFAULT NULL   COMMENT '轮换后替换本令牌的 jti',
    revoked_at      DATETIME     NULL     DEFAULT NULL   COMMENT '吊销时间',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by       BIGINT       NULL     DEFAULT NULL   COMMENT '创建人ID',
    update_by       BIGINT       NULL     DEFAULT NULL   COMMENT '更新人ID',
    deleted         TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    version         INT          NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_refresh_token_jti (jti),
    KEY idx_sys_refresh_token_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='刷新令牌';

-- 8. 操作日志
CREATE TABLE sys_operation_log (
    id                    BIGINT       NOT NULL                COMMENT '主键（雪花ID）',
    user_id               BIGINT       NULL     DEFAULT NULL   COMMENT '操作人ID（逻辑引用 sys_user.id）',
    username              VARCHAR(50)  NULL     DEFAULT NULL   COMMENT '操作人账号（冗余）',
    operation_type        VARCHAR(50)  NOT NULL                COMMENT '操作类型：LOGIN/LOGOUT/CREATE/UPDATE/DELETE/APPROVE/EXPORT/IMPORT/OTHER',
    operation_description TEXT         NULL                    COMMENT '操作描述',
    target_type           VARCHAR(50)  NULL     DEFAULT NULL   COMMENT '目标类型',
    target_id             BIGINT       NULL     DEFAULT NULL   COMMENT '目标ID（逻辑引用）',
    ip_address            VARCHAR(50)  NULL     DEFAULT NULL   COMMENT 'IP地址',
    user_agent            VARCHAR(255) NULL     DEFAULT NULL   COMMENT 'UA',
    request_method        VARCHAR(10)  NULL     DEFAULT NULL   COMMENT 'HTTP方法',
    request_uri           VARCHAR(255) NULL     DEFAULT NULL   COMMENT '请求URI',
    status                VARCHAR(20)  NOT NULL DEFAULT 'SUCCESS' COMMENT '执行结果：SUCCESS/FAILED',
    error_message         VARCHAR(500) NULL     DEFAULT NULL   COMMENT '异常信息',
    create_time           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by             BIGINT       NULL     DEFAULT NULL   COMMENT '创建人ID',
    update_by             BIGINT       NULL     DEFAULT NULL   COMMENT '更新人ID',
    deleted               TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    version               INT          NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
    PRIMARY KEY (id),
    KEY idx_sys_oplog_user (user_id),
    KEY idx_sys_oplog_type (operation_type),
    KEY idx_sys_oplog_ctime (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志';

-- 9. 系统配置
CREATE TABLE sys_config (
    id          BIGINT       NOT NULL                COMMENT '主键（雪花ID）',
    config_key  VARCHAR(100) NOT NULL                COMMENT '配置键',
    config_value VARCHAR(500) NULL    DEFAULT NULL   COMMENT '配置值',
    config_type VARCHAR(50)  NULL     DEFAULT 'STRING' COMMENT '值类型：STRING/INT/BOOL/JSON',
    description VARCHAR(255) NULL     DEFAULT NULL   COMMENT '描述',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by   BIGINT       NULL     DEFAULT NULL   COMMENT '创建人ID',
    update_by   BIGINT       NULL     DEFAULT NULL   COMMENT '更新人ID',
    deleted     TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    version     INT          NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_config_key (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置';

-- ======================================================================
-- 二、masterdata 域（biz_ 前缀）
-- ======================================================================

-- 10. 分类树
CREATE TABLE biz_category (
    id          BIGINT       NOT NULL                COMMENT '主键（雪花ID）',
    name        VARCHAR(100) NOT NULL                COMMENT '分类名',
    type        VARCHAR(20)  NOT NULL                COMMENT '业务类型：EXPENSE/MATERIAL/PROJECT/CONTACT/SEAL/PAYMENT/OTHER',
    code        VARCHAR(50)  NULL     DEFAULT NULL   COMMENT '分类编码',
    parent_id   BIGINT       NULL     DEFAULT NULL   COMMENT '父分类ID（逻辑引用）',
    sort_order  INT          NOT NULL DEFAULT 0      COMMENT '排序',
    enabled     TINYINT      NOT NULL DEFAULT 1      COMMENT '启用：0禁用 1启用',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by   BIGINT       NULL     DEFAULT NULL   COMMENT '创建人ID',
    update_by   BIGINT       NULL     DEFAULT NULL   COMMENT '更新人ID',
    deleted     TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    version     INT          NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
    PRIMARY KEY (id),
    KEY idx_biz_category_parent (parent_id),
    KEY idx_biz_category_type (type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='分类树';

-- 11. 往来单位
CREATE TABLE biz_contact (
    id                       BIGINT       NOT NULL                COMMENT '主键（雪花ID）',
    name                     VARCHAR(200) NOT NULL                COMMENT '单位名称',
    type                     VARCHAR(20)  NOT NULL                COMMENT '类型：SUPPLIER/CUSTOMER/SUBCONTRACTOR/OTHER',
    contact_person           VARCHAR(50)  NULL     DEFAULT NULL   COMMENT '联系人',
    phone                    VARCHAR(20)  NULL     DEFAULT NULL   COMMENT '手机号【需加密】',
    email                    VARCHAR(100) NULL     DEFAULT NULL   COMMENT '邮箱',
    address                  VARCHAR(255) NULL     DEFAULT NULL   COMMENT '地址',
    tax_identification_number VARCHAR(50) NULL    DEFAULT NULL   COMMENT '税号【需加密】',
    bank_account             VARCHAR(100) NULL     DEFAULT NULL   COMMENT '银行账号【需加密】',
    create_time              DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time              DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by                BIGINT       NULL     DEFAULT NULL   COMMENT '创建人ID',
    update_by                BIGINT       NULL     DEFAULT NULL   COMMENT '更新人ID',
    deleted                  TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    version                  INT          NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
    PRIMARY KEY (id),
    KEY idx_biz_contact_name (name),
    KEY idx_biz_contact_type (type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='往来单位';

-- 12. 材料库
CREATE TABLE biz_material (
    id            BIGINT        NOT NULL                COMMENT '主键（雪花ID）',
    material_name VARCHAR(200)  NOT NULL                COMMENT '材料名',
    material_code VARCHAR(50)   NOT NULL                COMMENT '材料编码',
    specification VARCHAR(200)  NULL     DEFAULT NULL   COMMENT '规格',
    unit          VARCHAR(20)   NULL     DEFAULT NULL   COMMENT '计量单位',
    current_price DECIMAL(18,2) NULL     DEFAULT NULL   COMMENT '当前单价',
    supplier_id   BIGINT        NULL     DEFAULT NULL   COMMENT '供应商ID（逻辑引用 biz_contact.id）',
    category_id   BIGINT        NULL     DEFAULT NULL   COMMENT '分类ID（逻辑引用 biz_category.id）',
    create_time   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by     BIGINT        NULL     DEFAULT NULL   COMMENT '创建人ID',
    update_by     BIGINT        NULL     DEFAULT NULL   COMMENT '更新人ID',
    deleted       TINYINT       NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    version       INT           NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
    PRIMARY KEY (id),
    UNIQUE KEY uk_biz_material_code (material_code),
    KEY idx_biz_material_supplier (supplier_id),
    KEY idx_biz_material_category (category_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='材料库';

-- 13. 设备台账
CREATE TABLE biz_equipment (
    id            BIGINT        NOT NULL                COMMENT '主键（雪花ID）',
    name          VARCHAR(200)  NOT NULL                COMMENT '设备名',
    type          VARCHAR(50)   NOT NULL                COMMENT '设备类型（代码值）',
    model         VARCHAR(100)  NULL     DEFAULT NULL   COMMENT '型号',
    status        VARCHAR(20)   NOT NULL DEFAULT 'IDLE' COMMENT '状态：IDLE/IN_USE/LEASED/MAINTENANCE/SCRAPPED',
    purchase_date DATETIME      NULL     DEFAULT NULL   COMMENT '购置日期',
    purchase_price DECIMAL(18,2) NULL    DEFAULT NULL   COMMENT '购置价格',
    create_time   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by     BIGINT        NULL     DEFAULT NULL   COMMENT '创建人ID',
    update_by     BIGINT        NULL     DEFAULT NULL   COMMENT '更新人ID',
    deleted       TINYINT       NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    version       INT           NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
    PRIMARY KEY (id),
    KEY idx_biz_equipment_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='设备台账';

-- 14. 设备租赁
CREATE TABLE biz_equipment_lease (
    id               BIGINT        NOT NULL                COMMENT '主键（雪花ID）',
    equipment_id     BIGINT        NOT NULL                COMMENT '设备ID（逻辑引用 biz_equipment.id）',
    project_id       BIGINT        NULL     DEFAULT NULL   COMMENT '项目ID（逻辑引用 biz_project.id）',
    lessor_id        BIGINT        NULL     DEFAULT NULL   COMMENT '出租方ID（逻辑引用 biz_contact.id）',
    lease_start_date DATETIME      NULL     DEFAULT NULL   COMMENT '租赁开始日期',
    lease_end_date   DATETIME      NULL     DEFAULT NULL   COMMENT '租赁结束日期',
    daily_rent       DECIMAL(18,2) NULL     DEFAULT NULL   COMMENT '日租金',
    total_rent       DECIMAL(18,2) NULL     DEFAULT NULL   COMMENT '总租金',
    payment_status   VARCHAR(20)   NULL     DEFAULT NULL   COMMENT '付款状态：UNPAID/PARTIAL/PAID',
    create_time      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by        BIGINT        NULL     DEFAULT NULL   COMMENT '创建人ID',
    update_by        BIGINT        NULL     DEFAULT NULL   COMMENT '更新人ID',
    deleted          TINYINT       NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    version          INT           NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
    PRIMARY KEY (id),
    KEY idx_biz_equipment_lease_equipment (equipment_id),
    KEY idx_biz_equipment_lease_project (project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='设备租赁';

-- ======================================================================
-- 三、project 域（biz_ 前缀）
-- ======================================================================

-- 15. 项目台账
CREATE TABLE biz_project (
    id              BIGINT        NOT NULL                COMMENT '主键（雪花ID）',
    project_name    VARCHAR(200)  NOT NULL                COMMENT '项目名',
    client_name     VARCHAR(200)  NOT NULL                COMMENT '客户名',
    project_address VARCHAR(500)  NULL     DEFAULT NULL   COMMENT '项目地址',
    project_type    VARCHAR(100)  NOT NULL                COMMENT '项目类型（代码值）',
    start_date      DATETIME      NULL     DEFAULT NULL   COMMENT '开工日期',
    end_date        DATETIME      NULL     DEFAULT NULL   COMMENT '竣工日期',
    contract_amount DECIMAL(18,2) NULL     DEFAULT NULL   COMMENT '合同金额',
    budget_amount   DECIMAL(18,2) NULL     DEFAULT NULL   COMMENT '预算金额',
    actual_amount   DECIMAL(18,2) NOT NULL DEFAULT 0.00   COMMENT '实际已发生金额',
    status          VARCHAR(20)   NOT NULL DEFAULT 'PLANNING' COMMENT '状态：PLANNING/IN_PROGRESS/PAUSED/COMPLETED/CLOSED',
    description     TEXT          NULL                    COMMENT '描述',
    manager_id      BIGINT        NULL     DEFAULT NULL   COMMENT '项目经理ID（逻辑引用 sys_user.id）',
    manager_name    VARCHAR(100)  NULL     DEFAULT NULL   COMMENT '项目经理姓名（冗余）',
    create_time     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by       BIGINT        NULL     DEFAULT NULL   COMMENT '创建人ID',
    update_by       BIGINT        NULL     DEFAULT NULL   COMMENT '更新人ID',
    deleted         TINYINT       NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    version         INT           NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
    PRIMARY KEY (id),
    KEY idx_biz_project_status (status),
    KEY idx_biz_project_client (client_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='项目台账';

-- 16. 预算
CREATE TABLE biz_budget (
    id               BIGINT        NOT NULL                COMMENT '主键（雪花ID）',
    budget_name      VARCHAR(255)  NOT NULL                COMMENT '预算名',
    amount           DECIMAL(18,2) NOT NULL                COMMENT '预算金额',
    project_id       BIGINT        NULL     DEFAULT NULL   COMMENT '项目ID（逻辑引用 biz_project.id）',
    contract_id      BIGINT        NULL     DEFAULT NULL   COMMENT '分包合同ID（逻辑引用 biz_subcontractor_contract.id）',
    start_date       DATETIME      NULL     DEFAULT NULL   COMMENT '开始日期',
    end_date         DATETIME      NULL     DEFAULT NULL   COMMENT '结束日期',
    status           VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：DRAFT/ACTIVE/CLOSED',
    used_amount      DECIMAL(18,2) NOT NULL DEFAULT 0.00   COMMENT '已用金额',
    remaining_amount DECIMAL(18,2) NULL     DEFAULT NULL   COMMENT '剩余金额',
    create_time      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by        BIGINT        NULL     DEFAULT NULL   COMMENT '创建人ID',
    update_by        BIGINT        NULL     DEFAULT NULL   COMMENT '更新人ID',
    deleted          TINYINT       NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    version          INT           NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
    PRIMARY KEY (id),
    KEY idx_biz_budget_project (project_id),
    KEY idx_biz_budget_contract (contract_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='预算';

-- 17. 变更请求
CREATE TABLE biz_change_request (
    id             BIGINT        NOT NULL                COMMENT '主键（雪花ID）',
    project_id     BIGINT        NOT NULL                COMMENT '项目ID（逻辑引用 biz_project.id）',
    request_type   VARCHAR(50)   NOT NULL                COMMENT '变更类型（代码值）',
    description    TEXT          NULL                    COMMENT '变更说明',
    estimated_cost DECIMAL(18,2) NULL     DEFAULT NULL   COMMENT '预估成本',
    status         VARCHAR(20)   NOT NULL DEFAULT 'PENDING' COMMENT '状态：DRAFT/PENDING/APPROVED/REJECTED',
    requested_by   BIGINT        NULL     DEFAULT NULL   COMMENT '申请人ID（逻辑引用 sys_user.id）',
    approved_by    BIGINT        NULL     DEFAULT NULL   COMMENT '审批人ID（逻辑引用 sys_user.id）',
    approved_at    DATETIME      NULL     DEFAULT NULL   COMMENT '审批时间',
    create_time    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by      BIGINT        NULL     DEFAULT NULL   COMMENT '创建人ID',
    update_by      BIGINT        NULL     DEFAULT NULL   COMMENT '更新人ID',
    deleted        TINYINT       NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    version        INT           NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
    PRIMARY KEY (id),
    KEY idx_biz_change_request_project (project_id),
    KEY idx_biz_change_request_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='变更请求';

-- 18. 进度款
CREATE TABLE biz_progress_payment (
    id                BIGINT        NOT NULL                COMMENT '主键（雪花ID）',
    project_id        BIGINT        NOT NULL                COMMENT '项目ID（逻辑引用 biz_project.id）',
    payment_period    VARCHAR(50)   NOT NULL                COMMENT '期次（如 第1期）',
    application_amount DECIMAL(18,2) NOT NULL               COMMENT '申请金额',
    approved_amount   DECIMAL(18,2) NULL     DEFAULT NULL   COMMENT '批准金额',
    payment_amount    DECIMAL(18,2) NULL     DEFAULT NULL   COMMENT '实付金额',
    application_date  DATETIME      NULL     DEFAULT NULL   COMMENT '申请日期',
    approval_date     DATETIME      NULL     DEFAULT NULL   COMMENT '审批日期',
    payment_date      DATETIME      NULL     DEFAULT NULL   COMMENT '付款日期',
    status            VARCHAR(20)   NOT NULL DEFAULT 'PENDING' COMMENT '状态：DRAFT/PENDING/APPROVED/REJECTED',
    application_by    BIGINT        NULL     DEFAULT NULL   COMMENT '申请人ID（逻辑引用 sys_user.id）',
    approved_by       BIGINT        NULL     DEFAULT NULL   COMMENT '审批人ID（逻辑引用 sys_user.id）',
    create_time       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by         BIGINT        NULL     DEFAULT NULL   COMMENT '创建人ID',
    update_by         BIGINT        NULL     DEFAULT NULL   COMMENT '更新人ID',
    deleted           TINYINT       NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    version           INT           NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
    PRIMARY KEY (id),
    KEY idx_biz_progress_payment_project (project_id),
    KEY idx_biz_progress_payment_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='进度款';

-- 19. 分包商
CREATE TABLE biz_subcontractor (
    id                       BIGINT       NOT NULL                COMMENT '主键（雪花ID）',
    name                     VARCHAR(200) NOT NULL                COMMENT '分包商名',
    contact_person           VARCHAR(50)  NULL     DEFAULT NULL   COMMENT '联系人',
    phone                    VARCHAR(20)  NULL     DEFAULT NULL   COMMENT '手机号【需加密】',
    address                  VARCHAR(255) NULL     DEFAULT NULL   COMMENT '地址',
    qualification_level      VARCHAR(50)  NULL     DEFAULT NULL   COMMENT '资质等级',
    credit_rating            VARCHAR(20)  NULL     DEFAULT NULL   COMMENT '信用评级',
    business_scope           TEXT         NULL                    COMMENT '经营范围',
    tax_identification_number VARCHAR(50) NULL    DEFAULT NULL   COMMENT '税号【需加密】',
    bank_account             VARCHAR(100) NULL     DEFAULT NULL   COMMENT '银行账号【需加密】',
    description              TEXT         NULL                    COMMENT '备注',
    create_time              DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time              DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by                BIGINT       NULL     DEFAULT NULL   COMMENT '创建人ID',
    update_by                BIGINT       NULL     DEFAULT NULL   COMMENT '更新人ID',
    deleted                  TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    version                  INT          NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
    PRIMARY KEY (id),
    KEY idx_biz_subcontractor_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='分包商';

-- 20. 分包合同
CREATE TABLE biz_subcontractor_contract (
    id               BIGINT        NOT NULL                COMMENT '主键（雪花ID）',
    subcontractor_id BIGINT        NOT NULL                COMMENT '分包商ID（逻辑引用 biz_subcontractor.id）',
    project_id       BIGINT        NULL     DEFAULT NULL   COMMENT '项目ID（逻辑引用 biz_project.id）',
    contract_name    VARCHAR(255)  NOT NULL                COMMENT '合同名',
    contract_number  VARCHAR(255)  NOT NULL                COMMENT '合同编号',
    contract_amount  DECIMAL(18,2) NOT NULL                COMMENT '合同金额',
    start_date       DATETIME      NULL     DEFAULT NULL   COMMENT '开始日期',
    end_date         DATETIME      NULL     DEFAULT NULL   COMMENT '结束日期',
    status           VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：DRAFT/ACTIVE/COMPLETED/TERMINATED',
    description      TEXT          NULL                    COMMENT '备注',
    file_path        VARCHAR(255)  NULL     DEFAULT NULL   COMMENT '合同文件路径',
    file_name        VARCHAR(255)  NULL     DEFAULT NULL   COMMENT '合同文件名',
    paid_amount      DECIMAL(18,2) NOT NULL DEFAULT 0.00   COMMENT '已支付金额',
    create_time      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by        BIGINT        NULL     DEFAULT NULL   COMMENT '创建人ID',
    update_by        BIGINT        NULL     DEFAULT NULL   COMMENT '更新人ID',
    deleted          TINYINT       NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    version          INT           NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
    PRIMARY KEY (id),
    KEY idx_biz_subcontractor_contract_sub (subcontractor_id),
    KEY idx_biz_subcontractor_contract_project (project_id),
    KEY idx_biz_subcontractor_contract_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='分包合同';

-- 21. 结算凭证
CREATE TABLE biz_settlement_proof (
    id               BIGINT        NOT NULL                COMMENT '主键（雪花ID）',
    contract_id      BIGINT        NOT NULL                COMMENT '分包合同ID（逻辑引用 biz_subcontractor_contract.id）',
    subcontractor_id BIGINT        NULL     DEFAULT NULL   COMMENT '分包商ID（逻辑引用 biz_subcontractor.id）',
    proof_name       VARCHAR(255)  NOT NULL                COMMENT '凭证名',
    proof_number     VARCHAR(255)  NOT NULL                COMMENT '凭证编号',
    proof_date       DATETIME      NULL     DEFAULT NULL   COMMENT '凭证日期',
    settlement_amount DECIMAL(18,2) NOT NULL               COMMENT '结算金额',
    file_path        VARCHAR(500)  NULL     DEFAULT NULL   COMMENT '凭证文件路径',
    file_name        VARCHAR(255)  NULL     DEFAULT NULL   COMMENT '凭证文件名',
    description      TEXT          NULL                    COMMENT '备注',
    create_time      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by        BIGINT        NULL     DEFAULT NULL   COMMENT '创建人ID',
    update_by        BIGINT        NULL     DEFAULT NULL   COMMENT '更新人ID',
    deleted          TINYINT       NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    version          INT           NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
    PRIMARY KEY (id),
    KEY idx_biz_settlement_proof_contract (contract_id),
    KEY idx_biz_settlement_proof_sub (subcontractor_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='结算凭证';

-- ======================================================================
-- 四、expense 域（biz_ 前缀）
-- ======================================================================

-- 22. 票据（报销单）
CREATE TABLE biz_receipt (
    id                     BIGINT        NOT NULL                COMMENT '主键（雪花ID）',
    receipt_number         VARCHAR(100)  NOT NULL                COMMENT '票据编号',
    user_id                BIGINT        NOT NULL                COMMENT '上报人ID（逻辑引用 sys_user.id）',
    reporter_id            BIGINT        NULL     DEFAULT NULL   COMMENT '报票人ID（逻辑引用 sys_user.id）',
    project_id             BIGINT        NULL     DEFAULT NULL   COMMENT '归属项目ID（逻辑引用 biz_project.id）',
    reporter_name          VARCHAR(100)  NULL     DEFAULT NULL   COMMENT '报票人姓名（冗余）',
    receipt_date           DATETIME      NULL     DEFAULT NULL   COMMENT '票据日期',
    amount                 DECIMAL(18,2) NOT NULL                COMMENT '金额',
    type                   VARCHAR(50)   NOT NULL                COMMENT '类型（代码值）',
    description            TEXT          NULL                    COMMENT '描述',
    file_path              VARCHAR(500)  NULL     DEFAULT NULL   COMMENT '票据文件路径',
    file_hash              VARCHAR(255)  NULL     DEFAULT NULL   COMMENT '文件哈希',
    ocr_data               TEXT          NULL                    COMMENT 'OCR识别数据',
    verification_status    VARCHAR(50)   NOT NULL DEFAULT 'PENDING' COMMENT '财务核验状态：PENDING/VERIFIED/REJECTED',
    verified_by            BIGINT        NULL     DEFAULT NULL   COMMENT '核验人ID（逻辑引用 sys_user.id）',
    verified_at            DATETIME      NULL     DEFAULT NULL   COMMENT '核验时间',
    payment_status         VARCHAR(50)   NOT NULL DEFAULT 'UNPAID' COMMENT '付款状态：UNPAID/PARTIAL/PAID',
    paid_amount            DECIMAL(18,2) NOT NULL DEFAULT 0.00   COMMENT '已支付金额',
    paid_at                DATETIME      NULL     DEFAULT NULL   COMMENT '支付时间',
    paid_by                BIGINT        NULL     DEFAULT NULL   COMMENT '支付人ID（逻辑引用 sys_user.id）',
    payment_method         VARCHAR(50)   NULL     DEFAULT NULL   COMMENT '支付方式：CASH/BANK_TRANSFER/WECHAT/ALIPAY/OTHER',
    current_approval_level INT           NULL     DEFAULT 0      COMMENT '当前审批级别',
    create_time            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by              BIGINT        NULL     DEFAULT NULL   COMMENT '创建人ID',
    update_by              BIGINT        NULL     DEFAULT NULL   COMMENT '更新人ID',
    deleted                TINYINT       NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    version                INT           NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
    PRIMARY KEY (id),
    KEY idx_biz_receipt_user (user_id),
    KEY idx_biz_receipt_project (project_id),
    KEY idx_biz_receipt_vstatus (verification_status),
    KEY idx_biz_receipt_number (receipt_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='票据（报销单）';

-- 23. 审批记录
CREATE TABLE biz_approval_record (
    id               BIGINT      NOT NULL                COMMENT '主键（雪花ID）',
    receipt_id       BIGINT      NOT NULL                COMMENT '票据ID（逻辑引用 biz_receipt.id）',
    approval_level   INT         NOT NULL DEFAULT 0      COMMENT '审批级别（第几级）',
    approver_role    VARCHAR(50) NULL     DEFAULT NULL   COMMENT '审批人角色（代码值）',
    approver_id      BIGINT      NULL     DEFAULT NULL   COMMENT '审批人ID（逻辑引用 sys_user.id）',
    approver_name    VARCHAR(100) NULL     DEFAULT NULL  COMMENT '审批人姓名（冗余）',
    status           VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '审批结果：PENDING/APPROVED/REJECTED',
    approval_comment VARCHAR(500) NULL     DEFAULT NULL  COMMENT '审批意见',
    approval_time    DATETIME    NULL     DEFAULT NULL   COMMENT '审批时间',
    create_time      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by        BIGINT      NULL     DEFAULT NULL   COMMENT '创建人ID',
    update_by        BIGINT      NULL     DEFAULT NULL   COMMENT '更新人ID',
    deleted          TINYINT     NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    version          INT         NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
    PRIMARY KEY (id),
    KEY idx_biz_approval_record_receipt (receipt_id),
    KEY idx_biz_approval_record_approver (approver_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审批记录';

-- 24. 财务核验
CREATE TABLE biz_receipt_verification (
    id                 BIGINT        NOT NULL                COMMENT '主键（雪花ID）',
    receipt_id         BIGINT        NOT NULL                COMMENT '票据ID（逻辑引用 biz_receipt.id）',
    payment_account_id BIGINT        NOT NULL                COMMENT '付款账户ID（逻辑引用 biz_payment_account.id）',
    amount             DECIMAL(18,2) NOT NULL                COMMENT '核验金额',
    payment_method     VARCHAR(50)   NULL     DEFAULT NULL   COMMENT '支付方式（代码值）',
    verification_time  DATETIME      NULL     DEFAULT NULL   COMMENT '核验时间',
    verified_by        BIGINT        NULL     DEFAULT NULL   COMMENT '核验人ID（逻辑引用 sys_user.id）',
    remark             VARCHAR(255)  NULL     DEFAULT NULL   COMMENT '备注',
    create_time        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by          BIGINT        NULL     DEFAULT NULL   COMMENT '创建人ID',
    update_by          BIGINT        NULL     DEFAULT NULL   COMMENT '更新人ID',
    deleted            TINYINT       NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    version            INT           NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
    PRIMARY KEY (id),
    KEY idx_biz_receipt_verification_receipt (receipt_id),
    KEY idx_biz_receipt_verification_account (payment_account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='财务核验';

-- ======================================================================
-- 五、payment 域（biz_ 前缀）
-- ======================================================================

-- 25. 收支流水（账户记录）
CREATE TABLE biz_account (
    id                BIGINT        NOT NULL                COMMENT '主键（雪花ID）',
    project_id        BIGINT        NULL     DEFAULT NULL   COMMENT '项目ID（逻辑引用 biz_project.id）',
    transaction_date  DATETIME      NOT NULL                COMMENT '交易日期',
    amount            DECIMAL(18,2) NOT NULL                COMMENT '金额',
    type              VARCHAR(20)   NOT NULL                COMMENT '类型：INCOME/EXPENSE',
    receipt_id        BIGINT        NULL     DEFAULT NULL   COMMENT '票据ID（逻辑引用 biz_receipt.id）',
    description       TEXT          NULL                    COMMENT '描述',
    status            VARCHAR(20)   NOT NULL DEFAULT 'DRAFT' COMMENT '状态：DRAFT/CONFIRMED/CANCELLED',
    payment_account_id BIGINT       NULL     DEFAULT NULL   COMMENT '付款账户ID（逻辑引用 biz_payment_account.id）',
    contract_id       BIGINT        NULL     DEFAULT NULL   COMMENT '分包合同ID（逻辑引用 biz_subcontractor_contract.id）',
    payment_type      VARCHAR(50)   NULL     DEFAULT NULL   COMMENT '支付类型（代码值）',
    allocation_status VARCHAR(20)   NULL     DEFAULT NULL   COMMENT '成本分摊状态：UNALLOCATED/PARTIAL/ALLOCATED',
    category_id       BIGINT        NULL     DEFAULT NULL   COMMENT '分类ID（逻辑引用 biz_category.id）',
    contact_id        BIGINT        NULL     DEFAULT NULL   COMMENT '往来单位ID（逻辑引用 biz_contact.id）',
    material_id       BIGINT        NULL     DEFAULT NULL   COMMENT '材料ID（逻辑引用 biz_material.id）',
    subcontractor_id  BIGINT        NULL     DEFAULT NULL   COMMENT '分包商ID（逻辑引用 biz_subcontractor.id）',
    create_time       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by         BIGINT        NULL     DEFAULT NULL   COMMENT '创建人ID',
    update_by         BIGINT        NULL     DEFAULT NULL   COMMENT '更新人ID',
    deleted           TINYINT       NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    version           INT           NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
    PRIMARY KEY (id),
    KEY idx_biz_account_project (project_id),
    KEY idx_biz_account_receipt (receipt_id),
    KEY idx_biz_account_payment_account (payment_account_id),
    KEY idx_biz_account_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='收支流水（账户记录）';

-- 26. 付款账户
CREATE TABLE biz_payment_account (
    id          BIGINT        NOT NULL                COMMENT '主键（雪花ID）',
    name        VARCHAR(100)  NOT NULL                COMMENT '账户名',
    type        VARCHAR(50)   NOT NULL                COMMENT '账户类型（代码值）',
    balance     DECIMAL(18,2) NOT NULL DEFAULT 0.00   COMMENT '余额',
    account_number VARCHAR(100) NULL   DEFAULT NULL   COMMENT '账号【需加密】',
    description TEXT          NULL                    COMMENT '描述',
    create_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by   BIGINT        NULL     DEFAULT NULL   COMMENT '创建人ID',
    update_by   BIGINT        NULL     DEFAULT NULL   COMMENT '更新人ID',
    deleted     TINYINT       NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    version     INT           NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='付款账户';

-- 27. 付款交易
CREATE TABLE biz_payment_transaction (
    id               BIGINT        NOT NULL                COMMENT '主键（雪花ID）',
    account_id       BIGINT        NOT NULL                COMMENT '付款账户ID（逻辑引用 biz_payment_account.id）',
    receipt_id       BIGINT        NULL     DEFAULT NULL   COMMENT '票据ID（逻辑引用 biz_receipt.id）',
    project_id       BIGINT        NULL     DEFAULT NULL   COMMENT '项目ID（逻辑引用 biz_project.id）',
    amount           DECIMAL(18,2) NOT NULL                COMMENT '金额',
    type             VARCHAR(20)   NOT NULL                COMMENT '类型：INCOME/EXPENSE',
    transaction_date DATETIME      NOT NULL                COMMENT '交易时间',
    description      TEXT          NULL                    COMMENT '描述',
    create_time      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by        BIGINT        NULL     DEFAULT NULL   COMMENT '创建人ID',
    update_by        BIGINT        NULL     DEFAULT NULL   COMMENT '更新人ID',
    deleted          TINYINT       NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    version          INT           NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
    PRIMARY KEY (id),
    KEY idx_biz_payment_transaction_account (account_id),
    KEY idx_biz_payment_transaction_receipt (receipt_id),
    KEY idx_biz_payment_transaction_date (transaction_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='付款交易';

-- 28. 成本分摊
CREATE TABLE biz_cost_allocation (
    id                 BIGINT        NOT NULL                COMMENT '主键（雪花ID）',
    account_record_id  BIGINT        NOT NULL                COMMENT '账户记录ID（逻辑引用 biz_account.id）',
    target_type        VARCHAR(20)   NOT NULL                COMMENT '分摊目标类型：PROJECT/DEPT/SERVICE/CONTRACT',
    target_id          BIGINT        NOT NULL                COMMENT '分摊目标ID（逻辑引用）',
    allocation_amount  DECIMAL(18,2) NOT NULL                COMMENT '分摊金额',
    allocation_ratio   DECIMAL(10,4) NULL     DEFAULT NULL   COMMENT '分摊比例',
    create_time        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by          BIGINT        NULL     DEFAULT NULL   COMMENT '创建人ID',
    update_by          BIGINT        NULL     DEFAULT NULL   COMMENT '更新人ID',
    deleted            TINYINT       NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    version            INT           NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
    PRIMARY KEY (id),
    KEY idx_biz_cost_allocation_account (account_record_id),
    KEY idx_biz_cost_allocation_target (target_type, target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='成本分摊';

-- ======================================================================
-- 六、bank 域（biz_ 前缀）
-- ======================================================================

-- 29. 银行流水
CREATE TABLE biz_bank_statement (
    id               BIGINT        NOT NULL                COMMENT '主键（雪花ID）',
    account_id       BIGINT        NOT NULL                COMMENT '付款账户ID（逻辑引用 biz_payment_account.id）',
    transaction_date DATETIME      NOT NULL                COMMENT '交易日期',
    amount           DECIMAL(18,2) NOT NULL                COMMENT '金额',
    type             VARCHAR(20)   NOT NULL                COMMENT '类型：INCOME/EXPENSE',
    description      VARCHAR(255)  NULL     DEFAULT NULL   COMMENT '描述',
    counterparty     VARCHAR(100)  NULL     DEFAULT NULL   COMMENT '对方户名',
    bank_reference   VARCHAR(100)  NULL     DEFAULT NULL   COMMENT '银行流水号',
    balance          DECIMAL(18,2) NULL     DEFAULT NULL   COMMENT '余额',
    status           VARCHAR(20)   NOT NULL DEFAULT 'UNRECONCILED' COMMENT '对账状态：UNRECONCILED/RECONCILED/MATCHED',
    create_time      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by        BIGINT        NULL     DEFAULT NULL   COMMENT '创建人ID',
    update_by        BIGINT        NULL     DEFAULT NULL   COMMENT '更新人ID',
    deleted          TINYINT       NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    version          INT           NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
    PRIMARY KEY (id),
    KEY idx_biz_bank_statement_account (account_id),
    KEY idx_biz_bank_statement_date (transaction_date),
    KEY idx_biz_bank_statement_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='银行流水';

-- 30. 总账
CREATE TABLE biz_general_ledger (
    id               BIGINT        NOT NULL                COMMENT '主键（雪花ID）',
    account_name     VARCHAR(200)  NOT NULL                COMMENT '账户名',
    account_number   VARCHAR(100)  NULL     DEFAULT NULL   COMMENT '账号【需加密】',
    bank_name        VARCHAR(100)  NULL     DEFAULT NULL   COMMENT '开户行',
    balance          DECIMAL(18,2) NULL     DEFAULT NULL   COMMENT '余额',
    account_type     VARCHAR(50)   NULL     DEFAULT NULL   COMMENT '账户类型（代码值）',
    summary          VARCHAR(500)  NULL     DEFAULT NULL   COMMENT '摘要',
    transaction_time DATETIME      NULL     DEFAULT NULL   COMMENT '交易时间',
    transaction_type VARCHAR(20)   NULL     DEFAULT NULL   COMMENT '交易类型：INCOME/EXPENSE',
    amount           DECIMAL(18,2) NULL     DEFAULT NULL   COMMENT '金额',
    status           VARCHAR(20)   NULL     DEFAULT NULL   COMMENT '状态（代码值）',
    create_time      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by        BIGINT        NULL     DEFAULT NULL   COMMENT '创建人ID',
    update_by        BIGINT        NULL     DEFAULT NULL   COMMENT '更新人ID',
    deleted          TINYINT       NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    version          INT           NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
    PRIMARY KEY (id),
    KEY idx_biz_general_ledger_account_name (account_name),
    KEY idx_biz_general_ledger_time (transaction_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='总账';

-- 31. 对账结果
CREATE TABLE biz_reconciliation_result (
    id                     BIGINT        NOT NULL                COMMENT '主键（雪花ID）',
    account_id             BIGINT        NOT NULL                COMMENT '付款账户ID（逻辑引用 biz_payment_account.id）',
    statement_id           BIGINT        NULL     DEFAULT NULL   COMMENT '银行流水ID（逻辑引用 biz_bank_statement.id）',
    transaction_id         BIGINT        NULL     DEFAULT NULL   COMMENT '付款交易ID（逻辑引用 biz_payment_transaction.id）',
    status                 VARCHAR(20)   NOT NULL                COMMENT '对账状态：MATCHED/UNMATCHED/PARTIAL_MATCHED',
    discrepancy_type       VARCHAR(50)   NULL     DEFAULT NULL   COMMENT '差异类型：MISSING_IN_SYSTEM/MISSING_IN_BANK/AMOUNT_MISMATCH/DATE_MISMATCH/DESCRIPTION_MISMATCH',
    bank_amount            DECIMAL(18,2) NULL     DEFAULT NULL   COMMENT '银行金额',
    system_amount          DECIMAL(18,2) NULL     DEFAULT NULL   COMMENT '系统金额',
    bank_date              DATETIME      NULL     DEFAULT NULL   COMMENT '银行交易日期',
    system_date            DATETIME      NULL     DEFAULT NULL   COMMENT '系统交易日期',
    bank_description       VARCHAR(255)  NULL     DEFAULT NULL   COMMENT '银行描述',
    system_description     VARCHAR(255)  NULL     DEFAULT NULL   COMMENT '系统描述',
    statement_reference    VARCHAR(100)  NULL     DEFAULT NULL   COMMENT '银行流水参考号',
    transaction_reference  VARCHAR(100)  NULL     DEFAULT NULL   COMMENT '系统交易参考号',
    resolution_status      VARCHAR(20)   NOT NULL DEFAULT 'UNRESOLVED' COMMENT '处理状态：UNRESOLVED/RESOLVED',
    resolution_notes       TEXT          NULL                    COMMENT '处理说明',
    reconciliation_date    DATETIME      NULL     DEFAULT NULL   COMMENT '对账时间',
    create_time            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by              BIGINT        NULL     DEFAULT NULL   COMMENT '创建人ID',
    update_by              BIGINT        NULL     DEFAULT NULL   COMMENT '更新人ID',
    deleted                TINYINT       NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    version                INT           NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
    PRIMARY KEY (id),
    KEY idx_biz_recon_account (account_id),
    KEY idx_biz_recon_statement (statement_id),
    KEY idx_biz_recon_transaction (transaction_id),
    KEY idx_biz_recon_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='对账结果';

-- ======================================================================
-- 七、seal 域（biz_ 前缀）
-- ======================================================================

-- 32. 电子印章
CREATE TABLE biz_electronic_seal (
    id                  BIGINT        NOT NULL                COMMENT '主键（雪花ID）',
    seal_name           VARCHAR(200)  NOT NULL                COMMENT '印章名',
    seal_type           VARCHAR(50)   NOT NULL                COMMENT '印章类型（代码值，如 公章/合同章/财务章）',
    seal_code           VARCHAR(100)  NULL     DEFAULT NULL   COMMENT '印章编码',
    description         TEXT          NULL                    COMMENT '描述',
    status              VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE/DISABLED/ARCHIVED',
    seal_image          VARCHAR(1000) NULL     DEFAULT NULL   COMMENT '印章图片',
    storage_type        VARCHAR(20)   NULL     DEFAULT NULL   COMMENT '存储类型：FILE/USB_DRIVE/CLOUD',
    seal_file_id        VARCHAR(255)  NULL     DEFAULT NULL   COMMENT '印章文件ID',
    usb_drive_id        VARCHAR(255)  NULL     DEFAULT NULL   COMMENT 'USB设备ID',
    password_protected  TINYINT       NOT NULL DEFAULT 0      COMMENT '是否密码保护：0否 1是',
    last_used_time      DATETIME      NULL     DEFAULT NULL   COMMENT '最后使用时间',
    usage_count         INT           NOT NULL DEFAULT 0      COMMENT '使用次数',
    create_time         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by           BIGINT        NULL     DEFAULT NULL   COMMENT '创建人ID',
    update_by           BIGINT        NULL     DEFAULT NULL   COMMENT '更新人ID',
    deleted             TINYINT       NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    version             INT           NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
    PRIMARY KEY (id),
    KEY idx_biz_electronic_seal_type (seal_type),
    KEY idx_biz_electronic_seal_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='电子印章';

-- 33. 用印申请
CREATE TABLE biz_electronic_seal_application (
    id               BIGINT        NOT NULL                COMMENT '主键（雪花ID）',
    seal_id          BIGINT        NOT NULL                COMMENT '印章ID（逻辑引用 biz_electronic_seal.id）',
    seal_name        VARCHAR(200)  NULL     DEFAULT NULL   COMMENT '印章名（冗余）',
    applicant_id     BIGINT        NOT NULL                COMMENT '申请人ID（逻辑引用 sys_user.id）',
    applicant_name   VARCHAR(100)  NULL     DEFAULT NULL   COMMENT '申请人姓名（冗余）',
    purpose          VARCHAR(500)  NULL     DEFAULT NULL   COMMENT '用印事由',
    status           VARCHAR(20)   NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/APPROVED/REJECTED/CANCELLED',
    project_id       BIGINT        NULL     DEFAULT NULL   COMMENT '项目ID（逻辑引用 biz_project.id）',
    project_name     VARCHAR(200)  NULL     DEFAULT NULL   COMMENT '项目名（冗余）',
    contract_name    VARCHAR(255)  NULL     DEFAULT NULL   COMMENT '合同名',
    contract_path    VARCHAR(500)  NULL     DEFAULT NULL   COMMENT '合同文件路径',
    stamp_position   VARCHAR(100)  NULL     DEFAULT NULL   COMMENT '用印位置',
    approver_id      BIGINT        NULL     DEFAULT NULL   COMMENT '审批人ID（逻辑引用 sys_user.id）',
    approver_name    VARCHAR(100)  NULL     DEFAULT NULL   COMMENT '审批人姓名（冗余）',
    approval_comment VARCHAR(500)  NULL     DEFAULT NULL   COMMENT '审批意见',
    apply_time       DATETIME      NULL     DEFAULT NULL   COMMENT '申请时间',
    approval_time    DATETIME      NULL     DEFAULT NULL   COMMENT '审批时间',
    create_time      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by        BIGINT        NULL     DEFAULT NULL   COMMENT '创建人ID',
    update_by        BIGINT        NULL     DEFAULT NULL   COMMENT '更新人ID',
    deleted          TINYINT       NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    version          INT           NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
    PRIMARY KEY (id),
    KEY idx_biz_seal_application_seal (seal_id),
    KEY idx_biz_seal_application_applicant (applicant_id),
    KEY idx_biz_seal_application_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用印申请';

-- 34. 用印审批
CREATE TABLE biz_stamp_approval (
    id                    BIGINT        NOT NULL                COMMENT '主键（雪花ID）',
    stamp_id              BIGINT        NOT NULL                COMMENT '用印申请ID（逻辑引用 biz_electronic_seal_application.id）',
    seal_id               BIGINT        NULL     DEFAULT NULL   COMMENT '印章ID（逻辑引用 biz_electronic_seal.id）',
    seal_name             VARCHAR(200)  NULL     DEFAULT NULL   COMMENT '印章名（冗余）',
    seal_code             VARCHAR(100)  NULL     DEFAULT NULL   COMMENT '印章编码（冗余）',
    seal_permission       VARCHAR(100)  NULL     DEFAULT NULL   COMMENT '印章权限',
    project_id            BIGINT        NULL     DEFAULT NULL   COMMENT '项目ID（逻辑引用 biz_project.id）',
    project_name          VARCHAR(200)  NULL     DEFAULT NULL   COMMENT '项目名（冗余）',
    contract_name         VARCHAR(255)  NULL     DEFAULT NULL   COMMENT '合同名',
    contract_path         VARCHAR(500)  NULL     DEFAULT NULL   COMMENT '合同文件路径',
    original_contract_path VARCHAR(500) NULL     DEFAULT NULL   COMMENT '原始合同路径',
    stamp_positions       VARCHAR(500)  NULL     DEFAULT NULL   COMMENT '用印位置（多位置）',
    applicant_id          BIGINT        NULL     DEFAULT NULL   COMMENT '申请人ID（逻辑引用 sys_user.id）',
    applicant_name        VARCHAR(100)  NULL     DEFAULT NULL   COMMENT '申请人姓名（冗余）',
    approver_id           BIGINT        NULL     DEFAULT NULL   COMMENT '审批人ID（逻辑引用 sys_user.id）',
    approver_name         VARCHAR(100)  NULL     DEFAULT NULL   COMMENT '审批人姓名（冗余）',
    status                VARCHAR(20)   NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/APPROVED/REJECTED',
    reject_reason         VARCHAR(500)  NULL     DEFAULT NULL   COMMENT '驳回原因',
    apply_time            DATETIME      NULL     DEFAULT NULL   COMMENT '申请时间',
    approval_time         DATETIME      NULL     DEFAULT NULL   COMMENT '审批时间',
    create_time           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by             BIGINT        NULL     DEFAULT NULL   COMMENT '创建人ID',
    update_by             BIGINT        NULL     DEFAULT NULL   COMMENT '更新人ID',
    deleted               TINYINT       NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    version               INT           NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
    PRIMARY KEY (id),
    KEY idx_biz_stamp_approval_stamp (stamp_id),
    KEY idx_biz_stamp_approval_seal (seal_id),
    KEY idx_biz_stamp_approval_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用印审批';

-- 35. 用印盖章记录
CREATE TABLE biz_electronic_seal_stamp (
    id            BIGINT        NOT NULL                COMMENT '主键（雪花ID）',
    seal_id       BIGINT        NOT NULL                COMMENT '印章ID（逻辑引用 biz_electronic_seal.id）',
    seal_name     VARCHAR(200)  NULL     DEFAULT NULL   COMMENT '印章名（冗余）',
    project_id    BIGINT        NULL     DEFAULT NULL   COMMENT '项目ID（逻辑引用 biz_project.id）',
    project_name  VARCHAR(200)  NULL     DEFAULT NULL   COMMENT '项目名（冗余）',
    contract_name VARCHAR(255)  NULL     DEFAULT NULL   COMMENT '合同名',
    contract_path VARCHAR(500)  NULL     DEFAULT NULL   COMMENT '合同文件路径',
    stamp_position VARCHAR(100) NULL     DEFAULT NULL   COMMENT '盖章位置',
    stamp_image   VARCHAR(1000) NULL     DEFAULT NULL   COMMENT '盖章图片',
    stamp_time    DATETIME      NULL     DEFAULT NULL   COMMENT '盖章时间',
    user_id       BIGINT        NULL     DEFAULT NULL   COMMENT '操作人ID（逻辑引用 sys_user.id）',
    user_name     VARCHAR(100)  NULL     DEFAULT NULL   COMMENT '操作人姓名（冗余）',
    ip_address    VARCHAR(50)   NULL     DEFAULT NULL   COMMENT 'IP地址',
    create_time   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by     BIGINT        NULL     DEFAULT NULL   COMMENT '创建人ID',
    update_by     BIGINT        NULL     DEFAULT NULL   COMMENT '更新人ID',
    deleted       TINYINT       NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    version       INT           NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
    PRIMARY KEY (id),
    KEY idx_biz_seal_stamp_seal (seal_id),
    KEY idx_biz_seal_stamp_project (project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用印盖章记录';

-- 36. 印章使用审计
CREATE TABLE biz_electronic_seal_usage (
    id            BIGINT        NOT NULL                COMMENT '主键（雪花ID）',
    seal_id       BIGINT        NOT NULL                COMMENT '印章ID（逻辑引用 biz_electronic_seal.id）',
    seal_name     VARCHAR(200)  NULL     DEFAULT NULL   COMMENT '印章名（冗余）',
    user_id       BIGINT        NULL     DEFAULT NULL   COMMENT '使用人ID（逻辑引用 sys_user.id）',
    user_name     VARCHAR(100)  NULL     DEFAULT NULL   COMMENT '使用人姓名（冗余）',
    purpose       VARCHAR(500)  NULL     DEFAULT NULL   COMMENT '使用事由',
    document_type VARCHAR(50)   NULL     DEFAULT NULL   COMMENT '文档类型（代码值）',
    document_path VARCHAR(500)  NULL     DEFAULT NULL   COMMENT '文档路径',
    usage_time    DATETIME      NULL     DEFAULT NULL   COMMENT '使用时间',
    ip_address    VARCHAR(50)   NULL     DEFAULT NULL   COMMENT 'IP地址',
    create_time   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by     BIGINT        NULL     DEFAULT NULL   COMMENT '创建人ID',
    update_by     BIGINT        NULL     DEFAULT NULL   COMMENT '更新人ID',
    deleted       TINYINT       NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    version       INT           NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
    PRIMARY KEY (id),
    KEY idx_biz_seal_usage_seal (seal_id),
    KEY idx_biz_seal_usage_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='印章使用审计';

-- ======================================================================
-- 八、notification 域（biz_ 前缀）
-- ======================================================================

-- 37. 通知
CREATE TABLE biz_notification (
    id                BIGINT        NOT NULL                COMMENT '主键（雪花ID）',
    title             VARCHAR(255)  NOT NULL                COMMENT '标题',
    content_type      VARCHAR(20)   NULL     DEFAULT NULL   COMMENT '内容类型（代码值，如 文本/链接）',
    content           TEXT          NULL                    COMMENT '内容',
    link_url          VARCHAR(1000) NULL     DEFAULT NULL   COMMENT '跳转链接',
    priority          VARCHAR(20)   NOT NULL DEFAULT 'NORMAL' COMMENT '优先级：LOW/NORMAL/HIGH/URGENT',
    notification_type VARCHAR(50)   NOT NULL                COMMENT '通知类型（代码值）',
    sender_id         BIGINT        NULL     DEFAULT NULL   COMMENT '发送人ID（逻辑引用 sys_user.id）',
    sender_name       VARCHAR(100)  NULL     DEFAULT NULL   COMMENT '发送人姓名（冗余）',
    valid_start_time  DATETIME      NULL     DEFAULT NULL   COMMENT '生效开始时间',
    valid_end_time    DATETIME      NULL     DEFAULT NULL   COMMENT '生效结束时间',
    status            VARCHAR(20)   NOT NULL DEFAULT 'DRAFT' COMMENT '状态：DRAFT/PUBLISHED/REVOKED',
    publish_time      DATETIME      NULL     DEFAULT NULL   COMMENT '发布时间',
    create_time       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by         BIGINT        NULL     DEFAULT NULL   COMMENT '创建人ID',
    update_by         BIGINT        NULL     DEFAULT NULL   COMMENT '更新人ID',
    deleted           TINYINT       NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    version           INT           NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
    PRIMARY KEY (id),
    KEY idx_biz_notification_type (notification_type),
    KEY idx_biz_notification_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知';

-- 38. 用户通知
CREATE TABLE biz_user_notification (
    id               BIGINT      NOT NULL                COMMENT '主键（雪花ID）',
    notification_id  BIGINT      NOT NULL                COMMENT '通知ID（逻辑引用 biz_notification.id）',
    user_id          BIGINT      NOT NULL                COMMENT '用户ID（逻辑引用 sys_user.id）',
    is_read          TINYINT     NOT NULL DEFAULT 0      COMMENT '是否已读：0未读 1已读',
    read_time        DATETIME    NULL     DEFAULT NULL   COMMENT '阅读时间',
    channel          VARCHAR(20) NULL     DEFAULT NULL   COMMENT '推送渠道：SYSTEM/EMAIL/SMS',
    push_status      VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '推送状态：PENDING/SENT/FAILED',
    push_time        DATETIME    NULL     DEFAULT NULL   COMMENT '推送时间',
    create_time      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by        BIGINT      NULL     DEFAULT NULL   COMMENT '创建人ID',
    update_by        BIGINT      NULL     DEFAULT NULL   COMMENT '更新人ID',
    deleted          TINYINT     NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    version          INT         NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
    PRIMARY KEY (id),
    KEY idx_biz_user_notification_notification (notification_id),
    KEY idx_biz_user_notification_user (user_id),
    KEY idx_biz_user_notification_read (is_read)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户通知';

-- 39. 用户通知偏好
CREATE TABLE biz_user_notification_preference (
    id                 BIGINT      NOT NULL                COMMENT '主键（雪花ID）',
    user_id            BIGINT      NOT NULL                COMMENT '用户ID（逻辑引用 sys_user.id）',
    notification_type  VARCHAR(50) NOT NULL                COMMENT '通知类型（代码值）',
    receive_system     TINYINT     NOT NULL DEFAULT 1      COMMENT '接收站内信：0否 1是',
    receive_email      TINYINT     NOT NULL DEFAULT 0      COMMENT '接收邮件：0否 1是',
    receive_sms        TINYINT     NOT NULL DEFAULT 0      COMMENT '接收短信：0否 1是',
    quiet_mode         TINYINT     NOT NULL DEFAULT 0      COMMENT '免打扰模式：0关闭 1开启',
    quiet_start_time   TIME        NULL     DEFAULT NULL   COMMENT '免打扰开始时间',
    quiet_end_time     TIME        NULL     DEFAULT NULL   COMMENT '免打扰结束时间',
    priority_threshold VARCHAR(20) NULL     DEFAULT NULL   COMMENT '优先级阈值：LOW/NORMAL/HIGH/URGENT',
    create_time        DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time        DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by          BIGINT      NULL     DEFAULT NULL   COMMENT '创建人ID',
    update_by          BIGINT      NULL     DEFAULT NULL   COMMENT '更新人ID',
    deleted            TINYINT     NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    version            INT         NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
    PRIMARY KEY (id),
    UNIQUE KEY uk_biz_notification_preference (user_id, notification_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户通知偏好';