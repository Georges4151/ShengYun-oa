-- ======================================================================
-- V54：劳动合同 / 分包合同接入 BPM 审批流
--
-- 说明：
--   1. 两张合同业务表增加审批状态快照与流程实例 ID：
--      approval_status 默认 NONE（未提交），唯一判断依据仍是 bpm_instance.status。
--   2. 新增两个默认流程模板（模板 id 9017/9018、已发布版本 id 9117/9118，
--      紧接 V51 的 9016/9116，取下一个空闲号）：
--        hr-contract-approval        劳动合同审批
--        subcontractor-contract-approval 分包合同审批
--   3. 全部幂等：模板按 code 判重，版本按 (template_id, version_no) 判重。
--   4. 审批节点统一 approveMode=ANY（或签）、dueHours=24，与既有模板一致。
--   5. 仅增量迁移，不修改历史脚本与既有合同业务状态字段 status。
-- ======================================================================

-- ----------------------------------------------------------------------
-- 一、合同业务表补充审批字段
-- ----------------------------------------------------------------------
ALTER TABLE hr_contract
    ADD COLUMN approval_status VARCHAR(32) NOT NULL DEFAULT 'NONE'
        COMMENT '审批状态快照：NONE/PENDING/APPROVED/REJECTED' AFTER status,
    ADD COLUMN bpm_instance_id BIGINT NULL DEFAULT NULL
        COMMENT '关联BPM流程实例ID（逻辑引用 bpm_instance.id）' AFTER approval_status;

ALTER TABLE biz_subcontractor_contract
    ADD COLUMN approval_status VARCHAR(32) NOT NULL DEFAULT 'NONE'
        COMMENT '审批状态快照：NONE/PENDING/APPROVED/REJECTED' AFTER status,
    ADD COLUMN bpm_instance_id BIGINT NULL DEFAULT NULL
        COMMENT '关联BPM流程实例ID（逻辑引用 bpm_instance.id）' AFTER approval_status;

-- ----------------------------------------------------------------------
-- 二、劳动合同审批模板（直属上级审批 → HR复核 → 结束）
-- ----------------------------------------------------------------------
INSERT INTO bpm_template (id, code, name, biz_type, current_version_id, status, deleted, version)
SELECT 9017, 'hr-contract-approval', '劳动合同审批', 'HR_CONTRACT', NULL, 'ENABLED', 0, 0
WHERE NOT EXISTS (SELECT 1 FROM bpm_template WHERE code = 'hr-contract-approval');

INSERT INTO bpm_template_version (id, template_id, version_no, schema_json, status, effective_time, deleted, version)
SELECT
    9117,
    9017,
    1,
    '{"nodes":[
        {"nodeKey":"start","nodeName":"发起","seqNo":1,"nodeType":"START"},
        {"nodeKey":"superior","nodeName":"直属上级审批","seqNo":2,"nodeType":"APPROVAL",
         "approveMode":"ANY","assignees":[{"type":"SUPERIOR"}],"dueHours":24},
        {"nodeKey":"hr_review","nodeName":"HR复核","seqNo":3,"nodeType":"APPROVAL",
         "approveMode":"ANY","assignees":[{"type":"ROLE","value":"SYS_ADMIN"}],"dueHours":24},
        {"nodeKey":"end","nodeName":"结束","seqNo":4,"nodeType":"END"}
    ]}',
    'PUBLISHED',
    NOW(),
    0,
    0
WHERE NOT EXISTS (
    SELECT 1 FROM bpm_template_version WHERE template_id = 9017 AND version_no = 1
);

UPDATE bpm_template
SET current_version_id = 9117
WHERE code = 'hr-contract-approval' AND current_version_id IS NULL;

-- ----------------------------------------------------------------------
-- 三、分包合同审批模板（部门审批 → 财务核验 → 结束）
-- ----------------------------------------------------------------------
INSERT INTO bpm_template (id, code, name, biz_type, current_version_id, status, deleted, version)
SELECT 9018, 'subcontractor-contract-approval', '分包合同审批', 'SUBCONTRACT_CONTRACT', NULL, 'ENABLED', 0, 0
WHERE NOT EXISTS (SELECT 1 FROM bpm_template WHERE code = 'subcontractor-contract-approval');

INSERT INTO bpm_template_version (id, template_id, version_no, schema_json, status, effective_time, deleted, version)
SELECT
    9118,
    9018,
    1,
    '{"nodes":[
        {"nodeKey":"start","nodeName":"发起","seqNo":1,"nodeType":"START"},
        {"nodeKey":"dept_leader","nodeName":"部门审批","seqNo":2,"nodeType":"APPROVAL",
         "approveMode":"ANY","assignees":[{"type":"DEPT_LEADER"}],"dueHours":24},
        {"nodeKey":"finance_review","nodeName":"财务核验","seqNo":3,"nodeType":"APPROVAL",
         "approveMode":"ANY","assignees":[{"type":"ROLE","value":"FINANCE"}],"dueHours":24},
        {"nodeKey":"end","nodeName":"结束","seqNo":4,"nodeType":"END"}
    ]}',
    'PUBLISHED',
    NOW(),
    0,
    0
WHERE NOT EXISTS (
    SELECT 1 FROM bpm_template_version WHERE template_id = 9018 AND version_no = 1
);

UPDATE bpm_template
SET current_version_id = 9118
WHERE code = 'subcontractor-contract-approval' AND current_version_id IS NULL;
