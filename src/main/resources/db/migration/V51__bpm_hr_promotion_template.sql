-- ======================================================================
-- V51：HR 晋升审批默认流程模板
--
-- 说明：
--   1. 模板 id 9016、已发布版本 id 9116（9008~9015 / 9108~9115 已被
--      V31 薪资模板、V32 绩效模板、V37 行政模板占用，取下一个空闲号）。
--   2. 全部幂等：模板按 code 判重，版本按 (template_id, version_no) 判重。
--   3. 审批节点统一 approveMode=ANY（或签）、dueHours=24。
-- ======================================================================

-- ----------------------------------------------------------------------
-- 晋升（HR_PROMOTION：上级审批 → 部门负责人 → HR复核 → 结束）
-- ----------------------------------------------------------------------
INSERT INTO bpm_template (id, code, name, biz_type, current_version_id, status, deleted, version)
SELECT 9016, 'HR_PROMOTION', '员工晋升审批', 'HR_PROMOTION', NULL, 'ENABLED', 0, 0
WHERE NOT EXISTS (SELECT 1 FROM bpm_template WHERE code = 'HR_PROMOTION');

INSERT INTO bpm_template_version (id, template_id, version_no, schema_json, status, effective_time, deleted, version)
SELECT
    9116,
    9016,
    1,
    '{"nodes":[
        {"nodeKey":"start","nodeName":"发起","seqNo":1,"nodeType":"START"},
        {"nodeKey":"superior","nodeName":"直属上级审批","seqNo":2,"nodeType":"APPROVAL",
         "approveMode":"ANY","assignees":[{"type":"SUPERIOR"}],"dueHours":24},
        {"nodeKey":"dept_leader","nodeName":"部门负责人审批","seqNo":3,"nodeType":"APPROVAL",
         "approveMode":"ANY","assignees":[{"type":"DEPT_LEADER"}],"dueHours":24},
        {"nodeKey":"hr_review","nodeName":"HR复核","seqNo":4,"nodeType":"APPROVAL",
         "approveMode":"ANY","assignees":[{"type":"ROLE","value":"SYS_ADMIN"}],"dueHours":24},
        {"nodeKey":"end","nodeName":"结束","seqNo":5,"nodeType":"END"}
    ]}',
    'PUBLISHED',
    NOW(),
    0,
    0
WHERE NOT EXISTS (
    SELECT 1 FROM bpm_template_version WHERE template_id = 9016 AND version_no = 1
);

UPDATE bpm_template
SET current_version_id = 9116
WHERE code = 'HR_PROMOTION' AND current_version_id IS NULL;
