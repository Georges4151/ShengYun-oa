-- ======================================================================
-- V28：HR 绩效模板与绩效考评单
--
-- 说明：
--   1. 主键为雪花 ID（BIGINT），不 AUTO_INCREMENT，与其他业务表保持一致。
--   2. 模板的考评项与系数规则以 JSON 落库，业务层负责校验：
--      SUM(items_json.weight) = 100，score_rules_json 区间连续且不重叠。
--   3. 考评单走 BPM 审批，bpm_instance_id 为实例逻辑引用，不建物理外键；
--      leader_id 在发起时快照，避免组织变更后考评人错位。
--   4. 只做增量新增表，不修改任何历史迁移与既有业务表。
-- ======================================================================

-- ----------------------------------------------------------------------
-- 1. 绩效模板（hr_performance_template）
-- ----------------------------------------------------------------------
CREATE TABLE hr_performance_template (
    id                BIGINT       NOT NULL                COMMENT '主键（雪花ID）',
    code              VARCHAR(64)  NOT NULL                COMMENT '模板编码（唯一）',
    name              VARCHAR(128) NOT NULL                COMMENT '模板名称',
    position_id       BIGINT       NULL     DEFAULT NULL   COMMENT '适用岗位ID（逻辑引用 hr_position.id，NULL表示通用）',
    period_type       VARCHAR(20)  NOT NULL                COMMENT '考核周期：MONTH/QUARTER',
    items_json        JSON         NULL     DEFAULT NULL   COMMENT '考评项：[{name,weight,minScore,maxScore,description}]，权重合计=100',
    score_rules_json  JSON         NULL     DEFAULT NULL   COMMENT '系数规则：[{minScore,maxScore,coefficient}]，区间连续且不重叠',
    total_weight      INT          NOT NULL DEFAULT 0      COMMENT '权重合计（冗余校验，写入时须等于100）',
    status            VARCHAR(20)  NOT NULL DEFAULT 'ENABLED' COMMENT '状态：ENABLED/DISABLED',
    create_time       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by         BIGINT       NULL     DEFAULT NULL   COMMENT '创建人ID',
    update_by         BIGINT       NULL     DEFAULT NULL   COMMENT '更新人ID',
    deleted           TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    version           INT          NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
    PRIMARY KEY (id),
    UNIQUE KEY uk_hr_perf_tpl_code (code),
    KEY idx_hr_perf_tpl_position (position_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='HR绩效模板';

-- ----------------------------------------------------------------------
-- 2. 绩效考评单（hr_performance_review）
-- ----------------------------------------------------------------------
CREATE TABLE hr_performance_review (
    id                BIGINT        NOT NULL                COMMENT '主键（雪花ID）',
    employee_id       BIGINT        NOT NULL                COMMENT '被考评人ID（逻辑引用 hr_employee.id）',
    template_id       BIGINT        NOT NULL                COMMENT '绩效模板ID（逻辑引用 hr_performance_template.id）',
    period            VARCHAR(16)   NOT NULL                COMMENT '考核周期（月 2026-11 / 季 2026-Q4）',
    leader_id         BIGINT        NULL     DEFAULT NULL   COMMENT '直属上级ID（发起时快照）',
    self_score        DECIMAL(6,2)  NULL     DEFAULT NULL   COMMENT '自评总分',
    self_comment      VARCHAR(1000) NULL     DEFAULT NULL   COMMENT '自评说明',
    self_submitted_at DATETIME      NULL     DEFAULT NULL   COMMENT '自评提交时间',
    leader_score      DECIMAL(6,2)  NULL     DEFAULT NULL   COMMENT '上级打分',
    leader_comment    VARCHAR(1000) NULL     DEFAULT NULL   COMMENT '上级评语',
    final_score       DECIMAL(6,2)  NULL     DEFAULT NULL   COMMENT '定稿分数',
    coefficient       DECIMAL(6,4)  NULL     DEFAULT NULL   COMMENT '绩效系数（由 score_rules_json 映射）',
    status            VARCHAR(20)   NOT NULL DEFAULT 'DRAFT' COMMENT '状态：DRAFT/SELF_SUBMITTED/LEADER_DONE/APPROVED/REJECTED',
    bpm_instance_id   BIGINT        NULL     DEFAULT NULL   COMMENT 'BPM流程实例ID（逻辑引用 bpm_instance.id）',
    create_time       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by         BIGINT        NULL     DEFAULT NULL   COMMENT '创建人ID',
    update_by         BIGINT        NULL     DEFAULT NULL   COMMENT '更新人ID',
    deleted           TINYINT       NOT NULL DEFAULT 0      COMMENT '逻辑删除：0未删除 1已删除',
    version           INT           NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
    PRIMARY KEY (id),
    UNIQUE KEY uk_hr_perf_review (employee_id, template_id, period),
    KEY idx_hr_perf_review_status (status),
    KEY idx_hr_perf_review_bpm (bpm_instance_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='HR绩效考评单';
