package com.ceams.module.report.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.math.BigDecimal;
import lombok.Data;

/** 绩效考核报表：单部门绩效行。 */
@Data
public class HrPerformanceDeptVO {

    /** 部门 ID；未分配部门的行返回 null。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long deptId;

    /** 部门名称。 */
    private String deptName;

    /** 评审单数。 */
    private long reviewCount;

    /** 定稿数（status = APPROVED）。 */
    private long finalizedCount;

    /** 平均定稿分（仅 APPROVED 且 final_score 非空，无数据取 0）。 */
    private BigDecimal avgFinalScore = BigDecimal.ZERO;
}
