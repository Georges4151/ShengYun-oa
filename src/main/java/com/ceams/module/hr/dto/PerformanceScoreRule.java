package com.ceams.module.hr.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Data;

/** 绩效系数规则（模板 score_rules_json 的元素模型）：分数落在哪段取哪段系数。 */
@Data
public class PerformanceScoreRule {

    /** 区间起值（含）。 */
    @NotNull(message = "系数规则起始分不能为空")
    @DecimalMin(value = "0", message = "系数规则起始分不能为负数")
    @DecimalMax(value = "100", message = "系数规则起始分不能超过 100")
    private BigDecimal minScore;

    /** 区间止值（各段首尾相接，最后一段包含 100）。 */
    @NotNull(message = "系数规则结束分不能为空")
    @DecimalMin(value = "0", message = "系数规则结束分不能为负数")
    @DecimalMax(value = "100", message = "系数规则结束分不能超过 100")
    private BigDecimal maxScore;

    /** 绩效系数。 */
    @NotNull(message = "绩效系数不能为空")
    @DecimalMin(value = "0", message = "绩效系数不能为负数")
    private BigDecimal coefficient;
}
