-- ======================================================================
-- V19：业务单据关联 BPM 实例（M2 第 1 步）
--
-- 说明：
--   1. biz_receipt（报销单）与 biz_change_request（项目变更）新增 bpm_instance_id，
--      作为「业务单据 → 流程实例」的唯一关联字段，逻辑引用 bpm_instance.id，不建外键。
--   2. 两列均可空：存量单据保持 NULL，灰度期旧审批字段继续写入，BPM 未接管前不受影响。
--   3. 索引按普通二级索引建（非唯一）：撤回后重新提报会生成新的实例，同一单据在生命周期内
--      可能先后关联多个实例，唯一约束会阻塞该场景。
--   4. 本迁移只加列与索引，不修改任何存量数据、不改动现有审批逻辑。
-- ======================================================================

-- ----------------------------------------------------------------------
-- 一、报销单（biz_receipt）
-- ----------------------------------------------------------------------
ALTER TABLE biz_receipt
    ADD COLUMN bpm_instance_id BIGINT NULL DEFAULT NULL COMMENT 'BPM 流程实例ID（逻辑引用 bpm_instance.id）',
    ADD KEY idx_biz_receipt_bpm_instance (bpm_instance_id);

-- ----------------------------------------------------------------------
-- 二、项目变更（biz_change_request）
-- ----------------------------------------------------------------------
ALTER TABLE biz_change_request
    ADD COLUMN bpm_instance_id BIGINT NULL DEFAULT NULL COMMENT 'BPM 流程实例ID（逻辑引用 bpm_instance.id）',
    ADD KEY idx_biz_change_request_bpm_instance (bpm_instance_id);
