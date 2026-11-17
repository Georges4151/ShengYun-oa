-- ======================================================================
-- V18：角色数据范围（sys_role.data_scope）
--
-- 说明：
--   1. 新增 data_scope 列，默认 SELF，取值 ALL / DEPT / SELF；
--   2. 仅将系统管理员 SYS_ADMIN 置为 ALL，其余角色保持 SELF；
--      FINANCE 等业务角色如需更大范围，由管理员后续通过角色接口调整；
--   3. 原计划的业务表字段迁移顺延为 V19__biz_bpm_instance_id.sql。
-- ======================================================================

ALTER TABLE sys_role
    ADD COLUMN data_scope VARCHAR(16) NOT NULL DEFAULT 'SELF'
        COMMENT '数据范围：ALL/DEPT/SELF';

UPDATE sys_role SET data_scope = 'ALL' WHERE code = 'SYS_ADMIN';
