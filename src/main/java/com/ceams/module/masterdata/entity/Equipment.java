package com.ceams.module.masterdata.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ceams.common.base.BaseEntity;
import com.ceams.module.masterdata.enums.EquipmentStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 设备台账（biz_equipment）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_equipment")
public class Equipment extends BaseEntity {

    /** 设备名。 */
    private String name;

    /** 设备类型（代码值）。 */
    private String type;

    /** 型号。 */
    private String model;

    /** 状态。 */
    private EquipmentStatus status;

    /** 购置日期。 */
    private LocalDateTime purchaseDate;

    /** 购置价格。 */
    private BigDecimal purchasePrice;
}