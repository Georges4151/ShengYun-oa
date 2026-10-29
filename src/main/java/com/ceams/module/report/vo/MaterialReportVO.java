package com.ceams.module.report.vo;

import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

/** 材料报表视图对象：按材料维度输出数量 / 金额。 */
@Data
public class MaterialReportVO {

    /** 材料明细列表。 */
    private List<MaterialReportItemVO> items;

    /** 材料总数（参与统计的材料条数）。 */
    private Long totalCount = 0L;

    /** 金额总计。 */
    private BigDecimal totalAmount = BigDecimal.ZERO;
}
