package com.ceams.module.masterdata.vo;

import com.ceams.module.masterdata.enums.EquipmentStatus;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 设备台账视图对象。
 */
@Data
public class EquipmentVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private String name;

    private String type;

    private String model;

    private EquipmentStatus status;

    private LocalDate purchaseDate;

    private BigDecimal purchasePrice;

    private LocalDateTime createTime;
}