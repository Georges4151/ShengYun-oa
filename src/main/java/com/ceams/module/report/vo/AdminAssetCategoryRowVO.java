package com.ceams.module.report.vo;

import java.math.BigDecimal;
import lombok.Data;

/** 行政资产分类聚合行（Mapper 内部使用）。 */
@Data
public class AdminAssetCategoryRowVO {

    /** 资产分类。 */
    private String category;

    /** 该分类资产数量。 */
    private Long count;

    /** 该分类购置金额合计。 */
    private BigDecimal totalValue;
}
