-- ======================================================================
-- V31：HR 薪资审批默认流程模板（薪资变动 / 薪资更正）
--
-- 说明：
--   1. 模板 id 9008~9009、已发布版本 id 9108~9109，紧接 V26 的 9003~9007。
--   2. 全部幂等：模板按 code 判重，版本按 (template_id, version_no) 判重。
--   3. 审批节点统一 approveMode=ANY（或签）、dueHours=24，与 M1 保持一致。
--   4. 仅增量新增模板，不修改任何历史迁移与既有流程。
-- ======================================================================

-- ----------------------------------------------------------------------
-- 一、薪资变动（HR_SALARY_CHANGE：财务复核 → 总经理审批 → 结束）
-- ----------------------------------------------------------------------
INSERT INTO bpm_template (id, code, name, biz_type, current_version_id, status, deleted, version)
SELECT 9008, 'HR_SALARY_CHANGE', '薪资变动审批', 'HR_SALARY_CHANGE', NULL, 'ENABLED', 0, 0
WHERE NOT EXISTS (SELECT 1 FROM bpm_template WHERE code = 'HR_SALARY_CHANGE');

INSERT INTO bpm_template_version (id, template_id, version_no, schema_json, status, effective_time, deleted, version)
SELECT
    9108,
    9008,
    1,
    '{"nodes":[
        {"nodeKey":"start","nodeName":"发起","seqNo":1,"nodeType":"START"},
        {"nodeKey":"finance_review","nodeName":"财务复核","seqNo":2,"nodeType":"APPROVAL",
         "approveMode":"ANY","assignees":[{"type":"ROLE","value":"FINANCE"}],"dueHours":24},
        {"nodeKey":"gm_approve","nodeName":"总经理审批","seqNo":3,"nodeType":"APPROVAL",
         "approveMode":"ANY","assignees":[{"type":"ROLE","value":"SYS_ADMIN"}],"dueHours":24},
        {"nodeKey":"end","nodeName":"结束","seqNo":4,"nodeType":"END"}
    ]}',
    'PUBLISHED',
    NOW(),
    0,
    0
WHERE NOT EXISTS (
    SELECT 1 FROM bpm_template_version WHERE template_id = 9008 AND version_no = 1
);

UPDATE bpm_template
SET current_version_id = 9108
WHERE code = 'HR_SALARY_CHANGE' AND current_version_id IS NULL;

-- ----------------------------------------------------------------------
-- 二、薪资更正（HR_SALARY_CORRECTION：财务复核 → 结束）
-- ----------------------------------------------------------------------
INSERT INTO bpm_template (id, code, name, biz_type, current_version_id, status, deleted, version)
SELECT 9009, 'HR_SALARY_CORRECTION', '薪资更正审批', 'HR_SALARY_CORRECTION', NULL, 'ENABLED', 0, 0
WHERE NOT EXISTS (SELECT 1 FROM bpm_template WHERE code = 'HR_SALARY_CORRECTION');

INSERT INTO bpm_template_version (id, template_id, version_no, schema_json, status, effective_time, deleted, version)
SELECT
    9109,
    9009,
    1,
    '{"nodes":[
        {"nodeKey":"start","nodeName":"发起","seqNo":1,"nodeType":"START"},
        {"nodeKey":"finance_review","nodeName":"财务复核","seqNo":2,"nodeType":"APPROVAL",
         "approveMode":"ANY","assignees":[{"type":"ROLE","value":"FINANCE"}],"dueHours":24},
        {"nodeKey":"end","nodeName":"结束","seqNo":3,"nodeType":"END"}
    ]}',
    'PUBLISHED',
    NOW(),
    0,
    0
WHERE NOT EXISTS (
    SELECT 1 FROM bpm_template_version WHERE template_id = 9009 AND version_no = 1
);

UPDATE bpm_template
SET current_version_id = 9109
WHERE code = 'HR_SALARY_CORRECTION' AND current_version_id IS NULL;
