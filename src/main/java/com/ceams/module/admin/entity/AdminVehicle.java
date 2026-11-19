package com.ceams.module.admin.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ceams.common.base.BaseEntity;
import com.ceams.module.admin.enums.VehicleStatus;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 行政车辆台账（admin_vehicle）。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("admin_vehicle")
public class AdminVehicle extends BaseEntity {

    /** 车牌号（唯一）。 */
    private String plateNo;

    /** 品牌型号。 */
    private String brandModel;

    /** 座位数。 */
    private Integer seats;

    /** 购置日期。 */
    private LocalDate purchaseDate;

    /** 保险到期日。 */
    private LocalDate insuranceExpire;

    /** 年检到期日。 */
    private LocalDate inspectionExpire;

    /** 状态：IDLE/USING/MAINTENANCE。 */
    private VehicleStatus status;

    /** 备注。 */
    private String remark;
}
