-- ======================================================================
-- V53：离职下属强制转交
--
-- 说明：
--   1. hr_employee_change 增加 resign_leader_id：离职申请（RESIGN）指定的
--      「下属转交上级」员工 ID，审批通过时把离职员工名下的在职下属批量改挂到该上级。
--   2. change_type 为 VARCHAR，无需扩展枚举定义；自动转交产生的变更记录使用
--      LEADER_TRANSFER 类型（由应用层写入）。
--   3. 本脚本为增量迁移，入库后不得修改。
-- ======================================================================

ALTER TABLE hr_employee_change
    ADD COLUMN resign_leader_id BIGINT NULL DEFAULT NULL
        COMMENT '离职时下属转交的上级员工ID（RESIGN 使用，逻辑引用 hr_employee.id）'
        AFTER target_level_id;
