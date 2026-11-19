package com.ceams.module.admin.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Data;

/** 易耗品申领请求。 */
@Data
public class MaterialApplyRequest {

    /** 物料 ID。 */
    @NotNull(message = "物料 ID 不能为空")
    private Long materialId;

    /** 申领数量（必须大于 0）。 */
    @NotNull(message = "申领数量不能为空")
    @DecimalMin(value = "0.00", inclusive = false, message = "申领数量必须大于 0")
    private BigDecimal quantity;

    /** 申领事由。 */
    private String reason;
}
