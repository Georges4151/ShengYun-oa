package com.ceams.module.hr.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Data;

/** 调整假期额度请求。 */
@Data
public class LeaveQuotaAdjustRequest {

    @NotNull(message = "总额度不能为空")
    @DecimalMin(value = "0.0", message = "总额度不能为负数")
    private BigDecimal totalDays;
}
