package com.ceams.module.admin.vo;

import java.math.BigDecimal;
import lombok.Data;

/** 资产台账汇总（只读聚合视图）。 */
@Data
public class AssetInventorySummaryVO {

    /** 资产总数。 */
    private long totalCount;

    /** 在库数量（IN_STOCK）。 */
    private long inStockCount;

    /** 借出数量（BORROWED）。 */
    private long borrowedCount;

    /** 维修中数量（REPAIRING）。 */
    private long repairingCount;

    /** 已报废数量（SCRAPPED）。 */
    private long scrappedCount;

    /** 购置金额合计（明文 BigDecimal）。 */
    private BigDecimal totalValue;

    /** 逾期未归还的借用记录数。 */
    private long overdueCount;
}
