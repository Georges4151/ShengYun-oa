-- ======================================================================
-- V5：expense 域增量 —— 为 biz_receipt 补充审批状态列
-- 三级审批状态机需表达：DRAFT / PENDING / APPROVED / REJECTED
-- ======================================================================

ALTER TABLE biz_receipt
    ADD COLUMN approval_status VARCHAR(20) NOT NULL DEFAULT 'DRAFT'
    COMMENT '审批状态：DRAFT/PENDING/APPROVED/REJECTED';

ALTER TABLE biz_receipt
    ADD KEY idx_biz_receipt_astatus (approval_status);
