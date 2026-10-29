package com.ceams.module.report.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.math.BigDecimal;
import lombok.Data;

/** 材料报表明细项。 */
@Data
public class MaterialReportItemVO {

    /** 材料 ID（逻辑引用 biz_material.id）。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long materialId;

    /** 材料编码。 */
    private String materialCode;

    /** 材料名。 */
    private String materialName;

    /** 规格。 */
    private String specification;

    /** 计量单位。 */
    private String unit;

    /** 当前单价。 */
    private BigDecimal currentPrice;

    /** 领用 / 采购笔数（关联 biz_account 的记录数）。 */
    private Long transactionCount;

    /** 金额合计（关联 biz_account.amount 汇总）。 */
    private BigDecimal totalAmount;
}
