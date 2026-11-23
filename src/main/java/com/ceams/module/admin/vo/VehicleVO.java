package com.ceams.module.admin.vo;

import com.ceams.module.admin.enums.VehicleStatus;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

/** 车辆台账展示对象。 */
@Data
public class VehicleVO {

    /** 车辆 ID。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 车牌号。 */
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

    /** 状态。 */
    private VehicleStatus status;

    /** 备注。 */
    private String remark;

    /** 保险剩余天数（负数表示已过期，可空）。 */
    private Long insuranceRemainDays;

    /** 年检剩余天数（负数表示已过期，可空）。 */
    private Long inspectionRemainDays;

    /** 创建时间。 */
    private LocalDateTime createTime;
}
