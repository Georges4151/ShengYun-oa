package com.ceams.module.report.vo;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/** 绩效考核报表视图对象：评审量、定稿量、平均分、分数分布与部门明细。 */
@Data
public class HrPerformanceReportVO {

    /** 考核周期（yyyy-MM 或 yyyy-Qn）。 */
    private String period;

    /** 评审单数。 */
    private long reviewCount;

    /** 定稿数（status = APPROVED）。 */
    private long finalizedCount;

    /** 平均定稿分（无数据取 0）。 */
    private BigDecimal avgFinalScore = BigDecimal.ZERO;

    /** 定稿分数分布（A/B/C/D 四桶）。 */
    private ScoreDistributionVO distribution = new ScoreDistributionVO();

    /** 按部门明细（deptId 升序，未分配部门排在最后）。 */
    private List<HrPerformanceDeptVO> byDept = new ArrayList<>();
}
