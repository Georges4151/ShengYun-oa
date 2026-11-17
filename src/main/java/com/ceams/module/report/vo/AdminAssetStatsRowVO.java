package com.ceams.module.report.vo;

import java.math.BigDecimal;
import lombok.Data;

/** 行政资产总量与状态分布聚合行（Mapper 内部使用）。 */
@Data
public class AdminAssetStatsRowVO {

    /** 资产总数。 */
    private Long totalCount;

    /** 购置金额合计。 */
    private BigDecimal totalValue;

    /** 在库数量。 */
    private Long inStockCount;

    /** 借出数量。 */
    private Long borrowedCount;

    /** 维修中数量。 */
    private Long repairingCount;

    /** 已报废数量。 */
    private Long scrappedCount;
}
