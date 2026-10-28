package com.ceams.module.payment.dto;

import com.ceams.module.payment.enums.TargetType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 新建成本分摊。
 */
@Data
public class CostAllocationCreateRequest {

    @NotNull(message = "账户记录ID不能为空")
    private Long accountRecordId;

    @NotNull(message = "分摊目标类型不能为空")
    private TargetType targetType;

    @NotNull(message = "分摊目标ID不能为空")
    private Long targetId;

    @NotNull(message = "分摊金额不能为空")
    @DecimalMin(value = "0", message = "分摊金额不能为负")
    private BigDecimal allocationAmount;

    private BigDecimal allocationRatio;
}
