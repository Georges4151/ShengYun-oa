package com.ceams.module.report.vo;

import java.math.BigDecimal;
import lombok.Data;

/** 绩效考核部门聚合行（Mapper 内部使用，Java 侧再计算平均分与总额）。 */
@Data
public class HrPerformanceDeptRowVO {

    /** 部门 ID。 */
    private Long deptId;

    /** 部门名称。 */
    private String deptName;

    /** 评审单数。 */
    private Long reviewCount;

    /** 定稿数。 */
    private Long finalizedCount;

    /** 定稿分数合计（仅 APPROVED 且 final_score 非空）。 */
    private BigDecimal sumFinalScore;

    /** A 档（≥ 90）人数。 */
    private Long bucketA;

    /** B 档（80 ~ 89.99）人数。 */
    private Long bucketB;

    /** C 档（70 ~ 79.99）人数。 */
    private Long bucketC;

    /** D 档（&lt; 70）人数。 */
    private Long bucketD;
}
