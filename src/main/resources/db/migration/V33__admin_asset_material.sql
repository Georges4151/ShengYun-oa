-- ======================================================================
-- V33：行政后勤 - 资产与易耗品（资产台账 / 借还 / 物料 / 申领）
--
-- 说明：
--   1. 主键为雪花 ID（BIGINT），不 AUTO_INCREMENT。
--   2. 资产状态与借还记录由 BPM 回调派生，业务侧不直接改状态。
--   3. 物料库存禁止为负，扣减统一走条件更新（stock_qty >= quantity）。
--   4. 仅增量新增表，不修改任何历史迁移与既有业务表。
-- ======================================================================

-- ----------------------------------------------------------------------
-- 1. 资产台账（admin_asset）
-- ----------------------------------------------------------------------
CREATE TABLE admin_asset (
    id                  BIGINT        NOT NULL                 COMMENT '主键（雪花ID）',
    asset_no            VARCHAR(64)   NOT NULL                 COMMENT '资产编号（AST-yyyyMM-####）',
    name                VARCHAR(128)  NOT NULL                 COMMENT '资产名称',
    category            VARCHAR(32)   NOT NULL DEFAULT 'OTHER' COMMENT '分类：ELECTRONIC/FURNITURE/VEHICLE_TOOL/OTHER',
    specification       VARCHAR(255)  NULL     DEFAULT NULL    COMMENT '规格型号',
    purchase_date       DATE          NULL     DEFAULT NULL    COMMENT '购置日期',
    purchase_price      DECIMAL(14,2) NULL     DEFAULT NULL    COMMENT '购置金额',
    dept_id             BIGINT        NULL     DEFAULT NULL    COMMENT '归属部门ID（逻辑引用 sys_dept.id）',
    keeper_id           BIGINT        NULL     DEFAULT NULL    COMMENT '保管人ID（sys_user.id）',
    location            VARCHAR(128)  NULL     DEFAULT NULL    COMMENT '存放位置',
    status              VARCHAR(20)   NOT NULL DEFAULT 'IN_STOCK' COMMENT '状态：IN_STOCK/BORROWED/REPAIRING/SCRAPPED',
    depreciation_months INT           NULL     DEFAULT NULL    COMMENT '折旧月数',
    remark              VARCHAR(500)  NULL     DEFAULT NULL    COMMENT '备注',
    create_time         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by           BIGINT        NULL     DEFAULT NULL    COMMENT '创建人ID',
    update_by           BIGINT        NULL     DEFAULT NULL    COMMENT '更新人ID',
    deleted             TINYINT       NOT NULL DEFAULT 0       COMMENT '逻辑删除：0未删除 1已删除',
    version             INT           NOT NULL DEFAULT 0       COMMENT '乐观锁版本号',
    PRIMARY KEY (id),
    UNIQUE KEY uk_admin_asset_no (asset_no),
    KEY idx_admin_asset_status (status),
    KEY idx_admin_asset_dept (dept_id),
    KEY idx_admin_asset_keeper (keeper_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='行政资产台账';

-- ----------------------------------------------------------------------
-- 2. 资产领用/借还记录（admin_asset_borrow）
-- ----------------------------------------------------------------------
CREATE TABLE admin_asset_borrow (
    id                   BIGINT       NOT NULL                COMMENT '主键（雪花ID）',
    asset_id             BIGINT       NOT NULL                COMMENT '资产ID（逻辑引用 admin_asset.id）',
    borrower_id          BIGINT       NOT NULL                COMMENT '借用人ID（sys_user.id）',
    borrow_date          DATE         NOT NULL                COMMENT '领用日期',
    expected_return_date DATE         NULL     DEFAULT NULL   COMMENT '预计归还日期',
    actual_return_date   DATE         NULL     DEFAULT NULL   COMMENT '实际归还日期',
    bpm_instance_id      BIGINT       NULL     DEFAULT NULL   COMMENT 'BPM实例ID',
    status               VARCHAR(20)  NOT NULL DEFAULT 'BORROWING' COMMENT '状态：BORROWING/RETURNED/REJECTED',
    remark               VARCHAR(500) NULL     DEFAULT NULL   COMMENT '备注',
    create_time          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by            BIGINT       NULL     DEFAULT NULL   COMMENT '创建人ID',
    update_by            BIGINT       NULL     DEFAULT NULL   COMMENT '更新人ID',
    deleted              TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    version              INT          NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
    PRIMARY KEY (id),
    KEY idx_admin_asset_borrow_asset (asset_id, status),
    KEY idx_admin_asset_borrow_user (borrower_id),
    KEY idx_admin_asset_borrow_bpm (bpm_instance_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='行政资产领用记录';

-- ----------------------------------------------------------------------
-- 3. 易耗品（admin_material）
-- ----------------------------------------------------------------------
CREATE TABLE admin_material (
    id           BIGINT        NOT NULL                COMMENT '主键（雪花ID）',
    code         VARCHAR(64)   NOT NULL                COMMENT '物料编码',
    name         VARCHAR(128)  NOT NULL                COMMENT '物料名称',
    specification VARCHAR(255) NULL     DEFAULT NULL   COMMENT '规格',
    unit         VARCHAR(16)   NULL     DEFAULT NULL   COMMENT '单位（个/盒/包）',
    stock_qty    DECIMAL(12,2) NOT NULL DEFAULT 0.00   COMMENT '当前库存（禁止为负）',
    safety_stock DECIMAL(12,2) NOT NULL DEFAULT 0.00   COMMENT '安全库存',
    unit_price   DECIMAL(14,2) NULL     DEFAULT NULL   COMMENT '参考单价',
    status       VARCHAR(20)   NOT NULL DEFAULT 'ENABLED' COMMENT '状态：ENABLED/DISABLED',
    create_time  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by    BIGINT        NULL     DEFAULT NULL   COMMENT '创建人ID',
    update_by    BIGINT        NULL     DEFAULT NULL   COMMENT '更新人ID',
    deleted      TINYINT       NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    version      INT           NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
    PRIMARY KEY (id),
    UNIQUE KEY uk_admin_material_code (code),
    KEY idx_admin_material_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='行政易耗品';

-- ----------------------------------------------------------------------
-- 4. 易耗品申领单（admin_material_apply）
-- ----------------------------------------------------------------------
CREATE TABLE admin_material_apply (
    id              BIGINT        NOT NULL                COMMENT '主键（雪花ID）',
    material_id     BIGINT        NOT NULL                COMMENT '物料ID（逻辑引用 admin_material.id）',
    applicant_id    BIGINT        NOT NULL                COMMENT '申请人ID（sys_user.id）',
    quantity        DECIMAL(12,2) NOT NULL                COMMENT '申领数量（>0）',
    reason          VARCHAR(500)  NULL     DEFAULT NULL   COMMENT '申领事由',
    bpm_instance_id BIGINT        NULL     DEFAULT NULL   COMMENT 'BPM实例ID',
    status          VARCHAR(20)   NOT NULL DEFAULT 'DRAFT' COMMENT '状态：DRAFT/APPROVING/APPROVED/REJECTED/ISSUED',
    issued_at       DATETIME      NULL     DEFAULT NULL   COMMENT '发放时间',
    issued_by       BIGINT        NULL     DEFAULT NULL   COMMENT '发放人ID',
    create_time     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by       BIGINT        NULL     DEFAULT NULL   COMMENT '创建人ID',
    update_by       BIGINT        NULL     DEFAULT NULL   COMMENT '更新人ID',
    deleted         TINYINT       NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    version         INT           NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
    PRIMARY KEY (id),
    KEY idx_admin_material_apply_material (material_id, status),
    KEY idx_admin_material_apply_user (applicant_id),
    KEY idx_admin_material_apply_bpm (bpm_instance_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='行政易耗品申领单';
