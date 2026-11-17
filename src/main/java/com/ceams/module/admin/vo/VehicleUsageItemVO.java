package com.ceams.module.admin.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.math.BigDecimal;
import lombok.Data;

/** 单台车辆用车汇总（只读聚合视图）。 */
@Data
public class VehicleUsageItemVO {

    /** 车辆 ID。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long vehicleId;

    /** 车牌号；车辆不存在时回落空串。 */
    private String plateNo;

    /** 区间内用车申请数。 */
    private long applyCount;

    /** 区间内还车里程合计（公里）。 */
    private BigDecimal totalMileage;

    /** 区间内车辆费用合计。 */
    private BigDecimal totalExpense;
}
