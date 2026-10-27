package com.ceams.module.masterdata.dto.equipment;

import com.ceams.module.masterdata.enums.EquipmentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 新建设备请求。
 */
@Data
public class EquipmentCreateRequest {

    @NotBlank(message = "设备名不能为空")
    @Size(max = 200, message = "设备名长度不能超过 200")
    private String name;

    @NotBlank(message = "设备类型不能为空")
    @Size(max = 50, message = "设备类型长度不能超过 50")
    private String type;

    @Size(max = 100, message = "型号长度不能超过 100")
    private String model;

    @NotNull(message = "设备状态不能为空")
    private EquipmentStatus status;

    private LocalDate purchaseDate;

    @PositiveOrZero(message = "购置价格不能为负")
    private BigDecimal purchasePrice;
}