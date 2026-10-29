package com.ceams.module.masterdata.dto.lease;

import com.ceams.module.masterdata.enums.PaymentStatus;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

/** 更新设备租赁请求（设备 ID 不可修改）。 */
@Data
public class EquipmentLeaseUpdateRequest {

    private Long projectId;

    private Long lessorId;

    private LocalDate leaseStartDate;

    private LocalDate leaseEndDate;

    @PositiveOrZero(message = "日租金不能为负")
    private BigDecimal dailyRent;

    @PositiveOrZero(message = "总租金不能为负")
    private BigDecimal totalRent;

    private PaymentStatus paymentStatus;
}
