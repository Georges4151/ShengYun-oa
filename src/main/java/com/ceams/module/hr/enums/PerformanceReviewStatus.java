package com.ceams.module.hr.enums;

/** 绩效考评单状态（hr_performance_review.status）。 */
public enum PerformanceReviewStatus {
    /** 待自评（批量生成后的初始状态）。 */
    DRAFT,
    /** 自评已提交，流程流转中。 */
    SELF_SUBMITTED,
    /** 直属上级已打分。 */
    LEADER_DONE,
    /** HR 已复核定稿 / 流程审批通过。 */
    APPROVED,
    /** 流程驳回、撤回或终止。 */
    REJECTED
}
