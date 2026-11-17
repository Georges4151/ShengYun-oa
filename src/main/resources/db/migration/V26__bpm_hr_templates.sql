-- ======================================================================
-- V26：HR 审批默认流程模板（转正 / 调岗 / 离职 / 请假 / 补卡）
--
-- 说明：
--   1. 模板 id 9003~9007、已发布版本 id 9103~9107，紧接 V21 的 9001/9002。
--   2. 全部幂等：模板按 code 判重，版本按 (template_id, version_no) 判重。
--   3. 审批节点统一 approveMode=ANY（或签）、dueHours=24。
--   4. 请假流程按 days 条件分支：days > 3 走 HR 复核，否则直接结束；
--      条件运算符取 BpmConditionEvaluator 支持的 GT。
-- ======================================================================

-- ----------------------------------------------------------------------
-- 一、转正（HR_REGULAR：上级审批 → HR复核 → 结束）
-- ----------------------------------------------------------------------
INSERT INTO bpm_template (id, code, name, biz_type, current_version_id, status, deleted, version)
SELECT 9003, 'HR_REGULAR', '员工转正审批', 'HR_REGULAR', NULL, 'ENABLED', 0, 0
WHERE NOT EXISTS (SELECT 1 FROM bpm_template WHERE code = 'HR_REGULAR');

INSERT INTO bpm_template_version (id, template_id, version_no, schema_json, status, effective_time, deleted, version)
SELECT
    9103,
    9003,
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
    SELECT 1 FROM bpm_template_version WHERE template_id = 9003 AND version_no = 1
);

UPDATE bpm_template
SET current_version_id = 9103
WHERE code = 'HR_REGULAR' AND current_version_id IS NULL;

-- ----------------------------------------------------------------------
-- 二、调岗（HR_TRANSFER：上级审批 → 部门负责人 → HR复核 → 结束）
-- ----------------------------------------------------------------------
INSERT INTO bpm_template (id, code, name, biz_type, current_version_id, status, deleted, version)
SELECT 9004, 'HR_TRANSFER', '员工调岗审批', 'HR_TRANSFER', NULL, 'ENABLED', 0, 0
WHERE NOT EXISTS (SELECT 1 FROM bpm_template WHERE code = 'HR_TRANSFER');

INSERT INTO bpm_template_version (id, template_id, version_no, schema_json, status, effective_time, deleted, version)
SELECT
    9104,
    9004,
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
    SELECT 1 FROM bpm_template_version WHERE template_id = 9004 AND version_no = 1
);

UPDATE bpm_template
SET current_version_id = 9104
WHERE code = 'HR_TRANSFER' AND current_version_id IS NULL;

-- ----------------------------------------------------------------------
-- 三、离职（HR_RESIGN：上级审批 → HR复核 → 结束）
-- ----------------------------------------------------------------------
INSERT INTO bpm_template (id, code, name, biz_type, current_version_id, status, deleted, version)
SELECT 9005, 'HR_RESIGN', '员工离职审批', 'HR_RESIGN', NULL, 'ENABLED', 0, 0
WHERE NOT EXISTS (SELECT 1 FROM bpm_template WHERE code = 'HR_RESIGN');

INSERT INTO bpm_template_version (id, template_id, version_no, schema_json, status, effective_time, deleted, version)
SELECT
    9105,
    9005,
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
    SELECT 1 FROM bpm_template_version WHERE template_id = 9005 AND version_no = 1
);

UPDATE bpm_template
SET current_version_id = 9105
WHERE code = 'HR_RESIGN' AND current_version_id IS NULL;

-- ----------------------------------------------------------------------
-- 四、请假（HR_LEAVE：上级审批 → 天数判断 → HR复核/结束）
-- ----------------------------------------------------------------------
INSERT INTO bpm_template (id, code, name, biz_type, current_version_id, status, deleted, version)
SELECT 9006, 'HR_LEAVE', '员工请假审批', 'HR_LEAVE', NULL, 'ENABLED', 0, 0
WHERE NOT EXISTS (SELECT 1 FROM bpm_template WHERE code = 'HR_LEAVE');

INSERT INTO bpm_template_version (id, template_id, version_no, schema_json, status, effective_time, deleted, version)
SELECT
    9106,
    9006,
    1,
    '{"nodes":[
        {"nodeKey":"start","nodeName":"发起","seqNo":1,"nodeType":"START"},
        {"nodeKey":"superior","nodeName":"直属上级审批","seqNo":2,"nodeType":"APPROVAL",
         "approveMode":"ANY","assignees":[{"type":"SUPERIOR"}],"dueHours":24,
         "nextNodeKey":"route_days"},
        {"nodeKey":"route_days","nodeName":"请假天数判断","seqNo":3,"nodeType":"CONDITION",
         "conditions":[
            {"field":"days","operator":"GT","value":"3","targetNodeKey":"hr_review"}
         ],
         "defaultNext":"end"},
        {"nodeKey":"hr_review","nodeName":"HR复核","seqNo":4,"nodeType":"APPROVAL",
         "approveMode":"ANY","assignees":[{"type":"ROLE","value":"SYS_ADMIN"}],"dueHours":24},
        {"nodeKey":"end","nodeName":"结束","seqNo":5,"nodeType":"END"}
    ]}',
    'PUBLISHED',
    NOW(),
    0,
    0
WHERE NOT EXISTS (
    SELECT 1 FROM bpm_template_version WHERE template_id = 9006 AND version_no = 1
);

UPDATE bpm_template
SET current_version_id = 9106
WHERE code = 'HR_LEAVE' AND current_version_id IS NULL;

-- ----------------------------------------------------------------------
-- 五、补卡（HR_CHECKIN_SUPPLEMENT：上级审批 → HR复核 → 结束）
-- ----------------------------------------------------------------------
INSERT INTO bpm_template (id, code, name, biz_type, current_version_id, status, deleted, version)
SELECT 9007, 'HR_CHECKIN_SUPPLEMENT', '补卡申请审批', 'HR_CHECKIN_SUPPLEMENT', NULL, 'ENABLED', 0, 0
WHERE NOT EXISTS (SELECT 1 FROM bpm_template WHERE code = 'HR_CHECKIN_SUPPLEMENT');

INSERT INTO bpm_template_version (id, template_id, version_no, schema_json, status, effective_time, deleted, version)
SELECT
    9107,
    9007,
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
    SELECT 1 FROM bpm_template_version WHERE template_id = 9007 AND version_no = 1
);

UPDATE bpm_template
SET current_version_id = 9107
WHERE code = 'HR_CHECKIN_SUPPLEMENT' AND current_version_id IS NULL;
