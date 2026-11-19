package com.ceams.module.report.vo;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;

/** 行政资产报表视图对象：总量、状态分布、分类统计与借用率。 */
@Data
public class AdminAssetReportVO {

    /** 资产总数。 */
    private long totalCount;

    /** 资产购置金额合计。 */
    private BigDecimal totalValue = BigDecimal.ZERO;

    /** 状态分布：IN_STOCK/BORROWED/REPAIRING/SCRAPPED 四键固定顺序，缺省 0。 */
    private Map<String, Long> byStatus = new LinkedHashMap<>();

    /** 按分类统计（category 升序）。 */
    private List<AdminAssetCategoryStatVO> byCategory = new ArrayList<>();

    /** 借用中记录数（borrow.status = BORROWING）。 */
    private long borrowingCount;

    /** 逾期未还记录数（借用中且预计归还日期早于今天）。 */
    private long overdueCount;

    /** 借用率（borrowingCount / totalCount，保留 4 位小数）。 */
    private BigDecimal borrowRate = BigDecimal.ZERO;
}
