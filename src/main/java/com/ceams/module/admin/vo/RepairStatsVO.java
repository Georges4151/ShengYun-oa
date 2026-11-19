package com.ceams.module.admin.vo;

import java.math.BigDecimal;
import lombok.Data;

/** 报修工单统计（只读聚合视图）。 */
@Data
public class RepairStatsVO {

    /** 工单总数。 */
    private long totalCount;

    /** 待受理数量（SUBMITTED）。 */
    private long submittedCount;

    /** 处理中数量（ASSIGNED + PROCESSING）。 */
    private long processingCount;

    /** 已完成数量（DONE + ACCEPTED）。 */
    private long completedCount;

    /** 完成率（0～1，4 位小数）。 */
    private BigDecimal completionRate;

    /** 平均处理时长（小时，assign → finish）。 */
    private BigDecimal avgHandleHours;
}
