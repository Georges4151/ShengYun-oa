package com.ceams.module.admin.dto;

import com.ceams.module.admin.enums.VehicleExpenseType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

/** 车辆费用登记请求。 */
@Data
public class VehicleExpenseRequest {

    /** 费用类型：MAINTENANCE/VIOLATION/FUEL。 */
    @NotNull(message = "费用类型不能为空")
    private VehicleExpenseType expenseType;

    /** 金额（>0）。 */
    @NotNull(message = "金额不能为空")
    @DecimalMin(value = "0.00", inclusive = false, message = "金额必须大于 0")
    private BigDecimal amount;

    /** 发生日期。 */
    @NotNull(message = "发生日期不能为空")
    private LocalDate occurDate;

    /** 附件（file 模块服务端文件名，可空）。 */
    private String attachmentName;

    /** 备注。 */
    private String remark;
}
