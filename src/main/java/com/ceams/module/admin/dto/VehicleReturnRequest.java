package com.ceams.module.admin.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/** 还车登记请求。 */
@Data
public class VehicleReturnRequest {

    /** 实际还车时间，为空取当前时间。 */
    private LocalDateTime returnTime;

    /** 还车里程（公里，≥0）。 */
    @NotNull(message = "还车里程不能为空")
    @DecimalMin(value = "0", message = "还车里程不能为负数")
    private BigDecimal mileage;

    /** 备注。 */
    private String remark;
}
