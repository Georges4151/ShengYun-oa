package com.ceams.module.masterdata.vo;

import com.ceams.module.masterdata.enums.PaymentStatus;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

/** 设备租赁视图对象。 */
@Data
public class EquipmentLeaseVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long equipmentId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long projectId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long lessorId;

    private LocalDate leaseStartDate;

    private LocalDate leaseEndDate;

    private BigDecimal dailyRent;

    private BigDecimal totalRent;

    private PaymentStatus paymentStatus;

    private LocalDateTime createTime;
}
