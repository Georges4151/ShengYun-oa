package com.ceams.module.report.vo;

import java.math.BigDecimal;
import lombok.Data;

/** 审批效率汇总行（Mapper 内部使用）。 */
@Data
public class BpmEfficiencySummaryRowVO {

    /** 流程实例数。 */
    private Long instanceCount;

    /** 已通过实例数。 */
    private Long approvedCount;

    /** 已通过实例的平均时长（秒）。 */
    private BigDecimal avgDurationSeconds;

    /** 任务总数。 */
    private Long taskCount;

    /** 超时任务数。 */
    private Long overdueTaskCount;
}
