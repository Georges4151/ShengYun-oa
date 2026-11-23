package com.ceams.module.admin.vo;

import com.ceams.module.admin.enums.MaterialStatus;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.math.BigDecimal;
import lombok.Data;

/** 易耗品展示对象。 */
@Data
public class MaterialVO {

    /** 物料 ID。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 物料编码。 */
    private String code;

    /** 物料名称。 */
    private String name;

    /** 规格。 */
    private String specification;

    /** 单位。 */
    private String unit;

    /** 当前库存。 */
    private BigDecimal stockQty;

    /** 安全库存。 */
    private BigDecimal safetyStock;

    /** 参考单价。 */
    private BigDecimal unitPrice;

    /** 状态。 */
    private MaterialStatus status;

    /** 库存缺口比例 (安全库存 - 当前库存) / 安全库存，用于低库存预警排序。 */
    private BigDecimal shortageRatio;
}
