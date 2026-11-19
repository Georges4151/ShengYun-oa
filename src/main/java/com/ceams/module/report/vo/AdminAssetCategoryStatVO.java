package com.ceams.module.report.vo;

import java.math.BigDecimal;
import lombok.Data;

/** 行政资产报表：单分类统计行。 */
@Data
public class AdminAssetCategoryStatVO {

    /** 资产分类：ELECTRONIC/FURNITURE/VEHICLE_TOOL/OTHER。 */
    private String category;

    /** 该分类资产数量。 */
    private long count;

    /** 该分类购置金额合计。 */
    private BigDecimal totalValue = BigDecimal.ZERO;
}
