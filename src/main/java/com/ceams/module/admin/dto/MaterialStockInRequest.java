package com.ceams.module.admin.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Data;

/** 易耗品入库请求。 */
@Data
public class MaterialStockInRequest {

    /** 入库数量（必须大于 0）。 */
    @NotNull(message = "入库数量不能为空")
    @DecimalMin(value = "0.00", inclusive = false, message = "入库数量必须大于 0")
    private BigDecimal quantity;

    /** 备注。 */
    private String remark;
}
