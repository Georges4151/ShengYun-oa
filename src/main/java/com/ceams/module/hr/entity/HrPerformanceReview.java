package com.ceams.module.hr.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ceams.common.base.BaseEntity;
import com.ceams.module.hr.enums.PerformanceReviewStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** HR 绩效考评单（hr_performance_review）。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hr_performance_review")
public class HrPerformanceReview extends BaseEntity {

    /** 被考评人 ID（逻辑引用 hr_employee.id）。 */
    private Long employeeId;

    /** 绩效模板 ID（逻辑引用 hr_performance_template.id）。 */
    private Long templateId;

    /** 考核周期（月 2026-11 / 季 2026-Q4）。 */
    private String period;

    /** 直属上级用户 ID（生成 / 发起时快照）。 */
    private Long leaderId;

    /** 自评明细 JSON：[{name,weight,score}]。 */
    private String selfItemsJson;

    /** 自评总分。 */
    private BigDecimal selfScore;

    /** 自评说明。 */
    private String selfComment;

    /** 自评提交时间。 */
    private LocalDateTime selfSubmittedAt;

    /** 上级评分明细 JSON：[{name,weight,score}]。 */
    private String leaderItemsJson;

    /** 上级打分。 */
    private BigDecimal leaderScore;

    /** 上级评语（HR 复核意见追加在此字段）。 */
    private String leaderComment;

    /** 定稿分数。 */
    private BigDecimal finalScore;

    /** 绩效系数（由模板 score_rules_json 映射）。 */
    private BigDecimal coefficient;

    /** 考评单状态。 */
    private PerformanceReviewStatus status;

    /** BPM 流程实例 ID（逻辑引用 bpm_instance.id）。 */
    private Long bpmInstanceId;
}
