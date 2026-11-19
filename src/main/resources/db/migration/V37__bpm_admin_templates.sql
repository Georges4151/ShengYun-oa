-- ======================================================================
-- V37：行政后勤审批默认流程模板（资产领用 / 用车 / 会议室 / 报修 / 易耗品申领）
--
-- 说明：
--   1. 模板 id 9011~9015、已发布版本 id 9111~9115，紧接 V32 的 9010/9110。
--   2. 全部幂等：模板按 code 判重，版本按 (template_id, version_no) 判重。
--   3. 审批节点统一为「部门主管（DEPT_LEADER）→ 行政审核（ROLE:SYS_ADMIN）」，
--      approveMode=ANY、dueHours=24，与 M1/M2 保持一致。
--   4. 部门主管解析为空时任务落 PENDING_ASSIGNEE，由行政在待办中心改派。
--   5. 仅增量新增模板，不修改任何历史迁移与既有流程。
-- ======================================================================

-- ----------------------------------------------------------------------
-- 一、资产领用（ADMIN_ASSET_BORROW）
-- ----------------------------------------------------------------------
INSERT INTO bpm_template (id, code, name, biz_type, current_version_id, status, deleted, version)
SELECT 9011, 'ADMIN_ASSET_BORROW', '资产领用审批', 'ADMIN_ASSET_BORROW', NULL, 'ENABLED', 0, 0
WHERE NOT EXISTS (SELECT 1 FROM bpm_template WHERE code = 'ADMIN_ASSET_BORROW');

INSERT INTO bpm_template_version (id, template_id, version_no, schema_json, status, effective_time, deleted, version)
SELECT
    9111,
    9011,
    1,
    '{"nodes":[
        {"nodeKey":"start","nodeName":"发起","seqNo":1,"nodeType":"START"},
        {"nodeKey":"dept_leader","nodeName":"部门主管","seqNo":2,"nodeType":"APPROVAL",
         "approveMode":"ANY","assignees":[{"type":"DEPT_LEADER"}],"dueHours":24},
        {"nodeKey":"admin_review","nodeName":"行政审核","seqNo":3,"nodeType":"APPROVAL",
         "approveMode":"ANY","assignees":[{"type":"ROLE","value":"SYS_ADMIN"}],"dueHours":24},
        {"nodeKey":"end","nodeName":"结束","seqNo":4,"nodeType":"END"}
    ]}',
    'PUBLISHED',
    NOW(),
    0,
    0
WHERE NOT EXISTS (
    SELECT 1 FROM bpm_template_version WHERE template_id = 9011 AND version_no = 1
);

UPDATE bpm_template
SET current_version_id = 9111
WHERE code = 'ADMIN_ASSET_BORROW' AND current_version_id IS NULL;

-- ----------------------------------------------------------------------
-- 二、用车申请（ADMIN_VEHICLE_APPLY）
-- ----------------------------------------------------------------------
INSERT INTO bpm_template (id, code, name, biz_type, current_version_id, status, deleted, version)
SELECT 9012, 'ADMIN_VEHICLE_APPLY', '用车申请审批', 'ADMIN_VEHICLE_APPLY', NULL, 'ENABLED', 0, 0
WHERE NOT EXISTS (SELECT 1 FROM bpm_template WHERE code = 'ADMIN_VEHICLE_APPLY');

INSERT INTO bpm_template_version (id, template_id, version_no, schema_json, status, effective_time, deleted, version)
SELECT
    9112,
    9012,
    1,
    '{"nodes":[
        {"nodeKey":"start","nodeName":"发起","seqNo":1,"nodeType":"START"},
        {"nodeKey":"dept_leader","nodeName":"部门主管","seqNo":2,"nodeType":"APPROVAL",
         "approveMode":"ANY","assignees":[{"type":"DEPT_LEADER"}],"dueHours":24},
        {"nodeKey":"admin_review","nodeName":"行政审核","seqNo":3,"nodeType":"APPROVAL",
         "approveMode":"ANY","assignees":[{"type":"ROLE","value":"SYS_ADMIN"}],"dueHours":24},
        {"nodeKey":"end","nodeName":"结束","seqNo":4,"nodeType":"END"}
    ]}',
    'PUBLISHED',
    NOW(),
    0,
    0
WHERE NOT EXISTS (
    SELECT 1 FROM bpm_template_version WHERE template_id = 9012 AND version_no = 1
);

UPDATE bpm_template
SET current_version_id = 9112
WHERE code = 'ADMIN_VEHICLE_APPLY' AND current_version_id IS NULL;

-- ----------------------------------------------------------------------
-- 三、会议室预定（ADMIN_MEETING_APPLY）
-- ----------------------------------------------------------------------
INSERT INTO bpm_template (id, code, name, biz_type, current_version_id, status, deleted, version)
SELECT 9013, 'ADMIN_MEETING_APPLY', '会议室预定审批', 'ADMIN_MEETING_APPLY', NULL, 'ENABLED', 0, 0
WHERE NOT EXISTS (SELECT 1 FROM bpm_template WHERE code = 'ADMIN_MEETING_APPLY');

INSERT INTO bpm_template_version (id, template_id, version_no, schema_json, status, effective_time, deleted, version)
SELECT
    9113,
    9013,
    1,
    '{"nodes":[
        {"nodeKey":"start","nodeName":"发起","seqNo":1,"nodeType":"START"},
        {"nodeKey":"dept_leader","nodeName":"部门主管","seqNo":2,"nodeType":"APPROVAL",
         "approveMode":"ANY","assignees":[{"type":"DEPT_LEADER"}],"dueHours":24},
        {"nodeKey":"admin_review","nodeName":"行政审核","seqNo":3,"nodeType":"APPROVAL",
         "approveMode":"ANY","assignees":[{"type":"ROLE","value":"SYS_ADMIN"}],"dueHours":24},
        {"nodeKey":"end","nodeName":"结束","seqNo":4,"nodeType":"END"}
    ]}',
    'PUBLISHED',
    NOW(),
    0,
    0
WHERE NOT EXISTS (
    SELECT 1 FROM bpm_template_version WHERE template_id = 9013 AND version_no = 1
);

UPDATE bpm_template
SET current_version_id = 9113
WHERE code = 'ADMIN_MEETING_APPLY' AND current_version_id IS NULL;

-- ----------------------------------------------------------------------
-- 四、报修申请（ADMIN_REPAIR）
-- ----------------------------------------------------------------------
INSERT INTO bpm_template (id, code, name, biz_type, current_version_id, status, deleted, version)
SELECT 9014, 'ADMIN_REPAIR', '报修申请审批', 'ADMIN_REPAIR', NULL, 'ENABLED', 0, 0
WHERE NOT EXISTS (SELECT 1 FROM bpm_template WHERE code = 'ADMIN_REPAIR');

INSERT INTO bpm_template_version (id, template_id, version_no, schema_json, status, effective_time, deleted, version)
SELECT
    9114,
    9014,
    1,
    '{"nodes":[
        {"nodeKey":"start","nodeName":"发起","seqNo":1,"nodeType":"START"},
        {"nodeKey":"dept_leader","nodeName":"部门主管","seqNo":2,"nodeType":"APPROVAL",
         "approveMode":"ANY","assignees":[{"type":"DEPT_LEADER"}],"dueHours":24},
        {"nodeKey":"admin_review","nodeName":"行政审核","seqNo":3,"nodeType":"APPROVAL",
         "approveMode":"ANY","assignees":[{"type":"ROLE","value":"SYS_ADMIN"}],"dueHours":24},
        {"nodeKey":"end","nodeName":"结束","seqNo":4,"nodeType":"END"}
    ]}',
    'PUBLISHED',
    NOW(),
    0,
    0
WHERE NOT EXISTS (
    SELECT 1 FROM bpm_template_version WHERE template_id = 9014 AND version_no = 1
);

UPDATE bpm_template
SET current_version_id = 9114
WHERE code = 'ADMIN_REPAIR' AND current_version_id IS NULL;

-- ----------------------------------------------------------------------
-- 五、易耗品申领（ADMIN_MATERIAL_APPLY）
-- ----------------------------------------------------------------------
INSERT INTO bpm_template (id, code, name, biz_type, current_version_id, status, deleted, version)
SELECT 9015, 'ADMIN_MATERIAL_APPLY', '易耗品申领审批', 'ADMIN_MATERIAL_APPLY', NULL, 'ENABLED', 0, 0
WHERE NOT EXISTS (SELECT 1 FROM bpm_template WHERE code = 'ADMIN_MATERIAL_APPLY');

INSERT INTO bpm_template_version (id, template_id, version_no, schema_json, status, effective_time, deleted, version)
SELECT
    9115,
    9015,
    1,
    '{"nodes":[
        {"nodeKey":"start","nodeName":"发起","seqNo":1,"nodeType":"START"},
        {"nodeKey":"dept_leader","nodeName":"部门主管","seqNo":2,"nodeType":"APPROVAL",
         "approveMode":"ANY","assignees":[{"type":"DEPT_LEADER"}],"dueHours":24},
        {"nodeKey":"admin_review","nodeName":"行政审核","seqNo":3,"nodeType":"APPROVAL",
         "approveMode":"ANY","assignees":[{"type":"ROLE","value":"SYS_ADMIN"}],"dueHours":24},
        {"nodeKey":"end","nodeName":"结束","seqNo":4,"nodeType":"END"}
    ]}',
    'PUBLISHED',
    NOW(),
    0,
    0
WHERE NOT EXISTS (
    SELECT 1 FROM bpm_template_version WHERE template_id = 9015 AND version_no = 1
);

UPDATE bpm_template
SET current_version_id = 9115
WHERE code = 'ADMIN_MATERIAL_APPLY' AND current_version_id IS NULL;
