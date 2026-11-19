-- ======================================================================
-- V17：IAM 最小组织字段
--
-- 说明：
--   1. 新增最小部门表 sys_dept，仅承载组织树（parent_id）与部门主管（leader_id），
--      供 BPM 审批人解析（DEPT_LEADER）与数据范围（DEPT=同部门及子部门）使用；
--      本期不提供部门维护界面，数据由运维/初始化脚本录入。
--   2. sys_user 增加 dept_id、leader_id 两列，均可空且带索引；不改动已有字段，
--      存量用户保持 dept_id/leader_id 为 NULL，BPM 解析自动降级，不破坏现有接口。
--   3. 沿用 V1 建库风格：雪花 ID（BIGINT，非 AUTO_INCREMENT）、逻辑引用不建外键、
--      统一 utf8mb4；审计列与 BaseEntity 保持一致。
-- ======================================================================

-- ----------------------------------------------------------------------
-- 一、部门（最小组织树）
-- ----------------------------------------------------------------------
CREATE TABLE sys_dept (
    id          BIGINT       NOT NULL                COMMENT '主键（雪花ID）',
    name        VARCHAR(100) NOT NULL                COMMENT '部门名称',
    parent_id   BIGINT       NULL     DEFAULT NULL   COMMENT '上级部门ID（逻辑引用 sys_dept.id，根部门为空）',
    leader_id   BIGINT       NULL     DEFAULT NULL   COMMENT '部门主管用户ID（逻辑引用 sys_user.id）',
    sort_order  INT          NOT NULL DEFAULT 0      COMMENT '同级排序号',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by   BIGINT       NULL     DEFAULT NULL   COMMENT '创建人ID',
    update_by   BIGINT       NULL     DEFAULT NULL   COMMENT '更新人ID',
    deleted     TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    version     INT          NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
    PRIMARY KEY (id),
    KEY idx_sys_dept_parent (parent_id),
    KEY idx_sys_dept_leader (leader_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='部门（最小组织树）';

-- ----------------------------------------------------------------------
-- 二、用户组织归属（可空，向后兼容）
-- ----------------------------------------------------------------------
ALTER TABLE sys_user
    ADD COLUMN dept_id   BIGINT NULL DEFAULT NULL COMMENT '所属部门ID（逻辑引用 sys_dept.id）',
    ADD COLUMN leader_id BIGINT NULL DEFAULT NULL COMMENT '直属上级用户ID（逻辑引用 sys_user.id）',
    ADD KEY idx_sys_user_dept (dept_id),
    ADD KEY idx_sys_user_leader (leader_id);
