-- ======================================================================
-- CEAMS 迁移（V4）
-- 说明：为变更请求表新增审批意见列，用于落库审批/驳回意见。
-- ======================================================================

ALTER TABLE biz_change_request
    ADD COLUMN approval_comment VARCHAR(500) NULL DEFAULT NULL COMMENT '审批意见' AFTER approved_at;
