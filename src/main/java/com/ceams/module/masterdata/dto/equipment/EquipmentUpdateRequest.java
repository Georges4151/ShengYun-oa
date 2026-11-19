package com.ceams.module.masterdata.dto.equipment;

import com.ceams.module.masterdata.enums.EquipmentStatus;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

/** 更新设备请求。 */
@Data
public class EquipmentUpdateRequest {

    @Size(max = 200, message = "设备名长度不能超过 200")
    private String name;

    @Size(max = 50, message = "设备类型长度不能超过 50")
    private String type;

    @Size(max = 100, message = "型号长度不能超过 100")
    private String model;

    private EquipmentStatus status;

    private LocalDate purchaseDate;

    @PositiveOrZero(message = "购置价格不能为负")
    private BigDecimal purchasePrice;
}
