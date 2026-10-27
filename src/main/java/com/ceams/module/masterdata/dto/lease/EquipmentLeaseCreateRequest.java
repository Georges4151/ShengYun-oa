package com.ceams.module.masterdata.dto.lease;

import com.ceams.module.masterdata.enums.PaymentStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 新建设备租赁请求。
 */
@Data
public class EquipmentLeaseCreateRequest {

    @NotNull(message = "设备 ID 不能为空")
    private Long equipmentId;

    @NotNull(message = "项目 ID 不能为空")
    private Long projectId;

    private Long lessorId;

    @NotNull(message = "租赁开始日期不能为空")
    private LocalDate leaseStartDate;

    @NotNull(message = "租赁结束日期不能为空")
    private LocalDate leaseEndDate;

    @PositiveOrZero(message = "日租金不能为负")
    private BigDecimal dailyRent;

    @PositiveOrZero(message = "总租金不能为负")
    private BigDecimal totalRent;

    private PaymentStatus paymentStatus;
}