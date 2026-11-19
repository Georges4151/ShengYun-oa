package com.ceams.module.admin.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Data;

/** 易耗品编辑请求（字段为空表示不修改，库存不在此接口调整）。 */
@Data
public class MaterialUpdateRequest {

    /** 物料 ID。 */
    @NotNull(message = "物料 ID 不能为空")
    private Long id;

    /** 物料编码。 */
    private String code;

    /** 物料名称。 */
    private String name;

    /** 规格。 */
    private String specification;

    /** 单位。 */
    private String unit;

    /** 安全库存。 */
    private BigDecimal safetyStock;

    /** 参考单价。 */
    private BigDecimal unitPrice;
}
