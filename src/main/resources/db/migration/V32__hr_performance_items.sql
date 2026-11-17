-- ======================================================================
-- V32：绩效考评明细列与绩效审批流程模板
--
-- 说明：
--   1. V28 建表时未包含自评/上级评分明细列，此处做增量补列（无需幂等，Flyway 只执行一次）。
--   2. 新增 HR_PERFORMANCE 流程模板（模板 9010 / 版本 9110），紧接 V31 的 9008~9009。
--   3. 模板写入完全沿用 V31 的幂等写法：模板按 code 判重，版本按 (template_id, version_no) 判重。
--   4. 仅增量新增，不修改任何历史迁移与既有流程。
-- ======================================================================

-- ----------------------------------------------------------------------
-- 一、考评单补列：自评 / 上级评分明细
-- ----------------------------------------------------------------------
ALTER TABLE hr_performance_review ADD COLUMN self_items_json JSON NULL COMMENT '自评明细：[{name,weight,score}]';
ALTER TABLE hr_performance_review ADD COLUMN leader_items_json JSON NULL COMMENT '上级评分明细';

-- ----------------------------------------------------------------------
-- 二、绩效考评审批（HR_PERFORMANCE：直属上级打分 → HR复核 → 结束）
-- ----------------------------------------------------------------------
INSERT INTO bpm_template (id, code, name, biz_type, current_version_id, status, deleted, version)
SELECT 9010, 'HR_PERFORMANCE', '绩效考评审批', 'HR_PERFORMANCE', NULL, 'ENABLED', 0, 0
WHERE NOT EXISTS (SELECT 1 FROM bpm_template WHERE code = 'HR_PERFORMANCE');

INSERT INTO bpm_template_version (id, template_id, version_no, schema_json, status, effective_time, deleted, version)
SELECT
    9110,
    9010,
    1,
    '{"nodes":[
        {"nodeKey":"start","nodeName":"发起","seqNo":1,"nodeType":"START"},
        {"nodeKey":"leader_score","nodeName":"直属上级打分","seqNo":2,"nodeType":"APPROVAL",
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
    SELECT 1 FROM bpm_template_version WHERE template_id = 9010 AND version_no = 1
);

UPDATE bpm_template
SET current_version_id = 9110
WHERE code = 'HR_PERFORMANCE' AND current_version_id IS NULL;
