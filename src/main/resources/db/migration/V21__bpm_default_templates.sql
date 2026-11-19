-- ======================================================================
-- V21：报销 / 项目变更默认流程模板（一次性迁移，无灰度开关）
--
-- 说明：
--   1. 本期不上线，直接全量切换：为 EXPENSE、CHANGE_REQUEST 各建一套默认模板
--      并发布为当前版本，新发起的单据全部走 BPM。
--   2. 报销流程固定为「部门审批 → 财务核验 → 结束」；财务核验是 BPM 审批节点，
--      通过即由 ExpenseBpmCallback 写 verification_status = VERIFIED。
--   3. 组织种子补齐：根部门「总公司」主管指向 admin、admin 归属该部门，
--      保证 DEPT_LEADER 能解析出审批人，不产生无人待办。
--   4. 权限：为 STAFF 补齐 BPM 菜单与查询权限，为 FINANCE 补齐任务处理权限，
--      全部使用 INSERT ... SELECT ... WHERE NOT EXISTS，脚本可重复执行。
--   5. 本迁移不修改电子印章相关表与逻辑。
-- ======================================================================

-- ----------------------------------------------------------------------
-- 一、报销模板（EXPENSE：部门审批 → 财务核验 → 结束）
-- ----------------------------------------------------------------------
INSERT INTO bpm_template (id, code, name, biz_type, current_version_id, status, deleted, version)
SELECT 9001, 'EXPENSE', '报销审批', 'EXPENSE', NULL, 'ENABLED', 0, 0
WHERE NOT EXISTS (SELECT 1 FROM bpm_template WHERE code = 'EXPENSE');

INSERT INTO bpm_template_version (id, template_id, version_no, schema_json, status, effective_time, deleted, version)
SELECT
    9101,
    9001,
    1,
    '{"nodes":[
        {"nodeKey":"start","nodeName":"发起","seqNo":1,"nodeType":"START"},
        {"nodeKey":"dept_approve","nodeName":"部门审批","seqNo":2,"nodeType":"APPROVAL",
         "approveMode":"ANY","assignees":[{"type":"DEPT_LEADER"}],"dueHours":24},
        {"nodeKey":"finance_verify","nodeName":"财务核验","seqNo":3,"nodeType":"APPROVAL",
         "approveMode":"ANY","assignees":[{"type":"ROLE","value":"FINANCE"}],"dueHours":24},
        {"nodeKey":"end","nodeName":"结束","seqNo":4,"nodeType":"END"}
    ]}',
    'PUBLISHED',
    NOW(),
    0,
    0
WHERE NOT EXISTS (
    SELECT 1 FROM bpm_template_version WHERE template_id = 9001 AND version_no = 1
);

UPDATE bpm_template
SET current_version_id = 9101
WHERE code = 'EXPENSE' AND current_version_id IS NULL;

-- ----------------------------------------------------------------------
-- 二、项目变更模板（CHANGE_REQUEST：部门审批 → 结束）
-- ----------------------------------------------------------------------
INSERT INTO bpm_template (id, code, name, biz_type, current_version_id, status, deleted, version)
SELECT 9002, 'CHANGE_REQUEST', '项目变更审批', 'CHANGE_REQUEST', NULL, 'ENABLED', 0, 0
WHERE NOT EXISTS (SELECT 1 FROM bpm_template WHERE code = 'CHANGE_REQUEST');

INSERT INTO bpm_template_version (id, template_id, version_no, schema_json, status, effective_time, deleted, version)
SELECT
    9102,
    9002,
    1,
    '{"nodes":[
        {"nodeKey":"start","nodeName":"发起","seqNo":1,"nodeType":"START"},
        {"nodeKey":"dept_approve","nodeName":"部门审批","seqNo":2,"nodeType":"APPROVAL",
         "approveMode":"ANY","assignees":[{"type":"DEPT_LEADER"}],"dueHours":24},
        {"nodeKey":"end","nodeName":"结束","seqNo":3,"nodeType":"END"}
    ]}',
    'PUBLISHED',
    NOW(),
    0,
    0
WHERE NOT EXISTS (
    SELECT 1 FROM bpm_template_version WHERE template_id = 9002 AND version_no = 1
);

UPDATE bpm_template
SET current_version_id = 9102
WHERE code = 'CHANGE_REQUEST' AND current_version_id IS NULL;

-- ----------------------------------------------------------------------
-- 三、组织种子：根部门主管 = admin，admin 归属根部门（幂等）
-- ----------------------------------------------------------------------
UPDATE sys_dept
SET leader_id = 1
WHERE parent_id IS NULL
  AND deleted = 0
  AND leader_id IS NULL
  AND EXISTS (SELECT 1 FROM sys_user WHERE id = 1 AND deleted = 0);

UPDATE sys_user
SET dept_id = (
    SELECT d.id FROM (SELECT id FROM sys_dept WHERE parent_id IS NULL AND deleted = 0 ORDER BY id LIMIT 1) d
)
WHERE id = 1
  AND deleted = 0
  AND dept_id IS NULL;

-- 3.1 单管理员环境兜底：admin 兼任财务角色，保证财务核验节点可解析出审批人
--     （本期未上线、仅内置 admin 一个账号；接入真实财务人员后可删除该绑定）
INSERT INTO sys_user_role (id, user_id, role_id)
SELECT (SELECT COALESCE(MAX(id), 0) + 1 FROM sys_user_role), 1, 2
WHERE EXISTS (SELECT 1 FROM sys_user WHERE id = 1 AND deleted = 0)
  AND NOT EXISTS (SELECT 1 FROM sys_user_role WHERE user_id = 1 AND role_id = 2);

-- ----------------------------------------------------------------------
-- 四、权限补授（幂等）
-- ----------------------------------------------------------------------
-- 4.1 STAFF（role_id = 3）：BPM 菜单、模板查看、实例查看、任务处理
INSERT INTO sys_role_permission (id, role_id, permission_id)
SELECT 1084, 3, 143
WHERE NOT EXISTS (SELECT 1 FROM sys_role_permission WHERE role_id = 3 AND permission_id = 143);

INSERT INTO sys_role_permission (id, role_id, permission_id)
SELECT 1085, 3, 144
WHERE NOT EXISTS (SELECT 1 FROM sys_role_permission WHERE role_id = 3 AND permission_id = 144);

INSERT INTO sys_role_permission (id, role_id, permission_id)
SELECT 1086, 3, 146
WHERE NOT EXISTS (SELECT 1 FROM sys_role_permission WHERE role_id = 3 AND permission_id = 146);

INSERT INTO sys_role_permission (id, role_id, permission_id)
SELECT 1087, 3, 147
WHERE NOT EXISTS (SELECT 1 FROM sys_role_permission WHERE role_id = 3 AND permission_id = 147);

-- 4.2 FINANCE（role_id = 2）：任务处理（财务核验节点）
INSERT INTO sys_role_permission (id, role_id, permission_id)
SELECT 1088, 2, 147
WHERE NOT EXISTS (SELECT 1 FROM sys_role_permission WHERE role_id = 2 AND permission_id = 147);
