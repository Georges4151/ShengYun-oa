-- ======================================================================
-- V55：报销额度分级规则（额度区间 → BPM 流程模板）
--
-- 说明：
--   1. 新增额度规则表 biz_expense_amount_rule：维度 = 金额区间 + 可选部门 + 可选项目，
--      每条规则映射一个 BPM 模板编码，并带优先级与启用状态；金额区间可自由设定。
--   2. 区间语义：min_amount 含、max_amount 不含；max_amount 为空表示无上限。
--      匹配优先级：项目规则 > 部门规则（含子部门）> 通用规则；
--      同优先级按 priority 倒序、min_amount 倒序取第一条。
--   3. 权限点 expense:amount-rule:manage 挂在 BPM 菜单 143 下（ID 续接 V52：
--      sys_permission 已用至 196、sys_role_permission 小号已用至 1138），
--      本脚本 sys_permission 使用 197、sys_role_permission 使用 1139。
--   4. 种子一条兜底规则：0 元以上无上限、不限部门/项目、映射 EXPENSE 模板、
--      priority=0、ENABLED，避免上线后所有报销因无规则可匹配而无法提交。
--      管理员后续可在「账号权限」中为其他角色单独勾选该权限点。
--   5. 所有语句使用 INSERT ... SELECT ... WHERE NOT EXISTS，可重复执行。
--   6. 本脚本为增量迁移，入库后不得修改；规则/权限变更另起更高版本号脚本。
-- ======================================================================

-- ----------------------------------------------------------------------
-- 一、额度规则表
-- ----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS biz_expense_amount_rule (
    id            BIGINT        NOT NULL                  COMMENT '主键（雪花ID）',
    name          VARCHAR(100)  NOT NULL                  COMMENT '规则名称',
    min_amount    DECIMAL(18,2) NOT NULL                  COMMENT '金额区间下限（含）',
    max_amount    DECIMAL(18,2) NULL     DEFAULT NULL     COMMENT '金额区间上限（不含），NULL 表示无上限',
    dept_id       BIGINT        NULL     DEFAULT NULL     COMMENT '适用部门ID（逻辑引用 sys_dept.id，匹配时含子部门；NULL 表示不限部门）',
    project_id    BIGINT        NULL     DEFAULT NULL     COMMENT '适用项目ID（逻辑引用 biz_project.id；NULL 表示不限项目）',
    template_code VARCHAR(64)   NOT NULL                  COMMENT 'BPM 流程模板编码（逻辑引用 bpm_template.code）',
    priority      INT           NOT NULL DEFAULT 0        COMMENT '优先级（同维度/同层级内数值越大越优先）',
    status        VARCHAR(20)   NOT NULL DEFAULT 'ENABLED' COMMENT '状态：ENABLED启用 / DISABLED停用',
    remark        VARCHAR(255)  NULL     DEFAULT NULL     COMMENT '备注',
    create_time   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by     BIGINT        NULL     DEFAULT NULL     COMMENT '创建人ID',
    update_by     BIGINT        NULL     DEFAULT NULL     COMMENT '更新人ID',
    deleted       TINYINT       NOT NULL DEFAULT 0        COMMENT '逻辑删除：0未删除 1已删除',
    version       INT           NOT NULL DEFAULT 0        COMMENT '乐观锁版本号',
    PRIMARY KEY (id),
    KEY idx_expense_amount_rule_status (status, deleted),
    KEY idx_expense_amount_rule_amount (min_amount, max_amount),
    KEY idx_expense_amount_rule_dept (dept_id, deleted),
    KEY idx_expense_amount_rule_project (project_id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='报销额度分级规则（额度区间 → BPM 流程模板）';

-- ----------------------------------------------------------------------
-- 二、权限点（sys_permission）
-- ----------------------------------------------------------------------
INSERT INTO sys_permission (id, code, name, type, parent_id, path, icon, sort_order, deleted, version)
SELECT 197, 'expense:amount-rule:manage', '报销额度分级规则维护', 'BUTTON', 143, NULL, NULL, 53, 0, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'expense:amount-rule:manage');

-- ----------------------------------------------------------------------
-- 三、角色-权限矩阵（sys_role_permission，仅 SYS_ADMIN）
-- ----------------------------------------------------------------------
INSERT INTO sys_role_permission (id, role_id, permission_id)
SELECT 1139, 1, 197
WHERE NOT EXISTS (SELECT 1 FROM sys_role_permission WHERE role_id = 1 AND permission_id = 197);

-- ----------------------------------------------------------------------
-- 四、兜底规则（0 元以上无上限、不限部门/项目、EXPENSE 模板）
-- ----------------------------------------------------------------------
INSERT INTO biz_expense_amount_rule
    (id, name, min_amount, max_amount, dept_id, project_id, template_code, priority, status, remark,
     create_time, update_time, deleted, version)
SELECT 9201,
       '报销默认审批（兜底）',
       0.00,
       NULL,
       NULL,
       NULL,
       'EXPENSE',
       0,
       'ENABLED',
       '兜底规则：未配置其他额度分级规则时，所有报销金额均使用 EXPENSE 流程模板',
       NOW(),
       NOW(),
       0,
       0
WHERE NOT EXISTS (
    SELECT 1 FROM biz_expense_amount_rule
    WHERE dept_id IS NULL
      AND project_id IS NULL
      AND min_amount = 0.00
      AND max_amount IS NULL
      AND deleted = 0
);
