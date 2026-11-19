package com.ceams.module.admin.dto;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import lombok.Data;

/** 易耗品建档请求。 */
@Data
public class MaterialCreateRequest {

    /** 物料编码（唯一）。 */
    @NotBlank(message = "物料编码不能为空")
    private String code;

    /** 物料名称。 */
    @NotBlank(message = "物料名称不能为空")
    private String name;

    /** 规格。 */
    private String specification;

    /** 单位。 */
    private String unit;

    /** 初始库存，为空按 0 处理。 */
    private BigDecimal stockQty;

    /** 安全库存，为空按 0 处理。 */
    private BigDecimal safetyStock;

    /** 参考单价。 */
    private BigDecimal unitPrice;
}
