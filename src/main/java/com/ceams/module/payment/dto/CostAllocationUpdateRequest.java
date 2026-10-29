package com.ceams.module.payment.dto;

import com.ceams.module.payment.enums.TargetType;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;
import lombok.Data;

/** 更新成本分摊（accountRecordId 不可变）。 */
@Data
public class CostAllocationUpdateRequest {

    private TargetType targetType;

    private Long targetId;

    @DecimalMin(value = "0", message = "分摊金额不能为负")
    private BigDecimal allocationAmount;

    private BigDecimal allocationRatio;
}
