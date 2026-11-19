package com.ceams.module.report.vo;

import lombok.Data;

/** 行政资产借用统计聚合行（Mapper 内部使用）。 */
@Data
public class AdminBorrowStatsRowVO {

    /** 借用中记录数。 */
    private Long borrowingCount;

    /** 逾期未还记录数。 */
    private Long overdueCount;
}
