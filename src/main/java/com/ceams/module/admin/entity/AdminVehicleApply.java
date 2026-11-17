package com.ceams.module.admin.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ceams.common.base.BaseEntity;
import com.ceams.module.admin.enums.VehicleApplyStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 行政用车申请（admin_vehicle_apply）。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("admin_vehicle_apply")
public class AdminVehicleApply extends BaseEntity {

    /** 车辆 ID（逻辑引用 admin_vehicle.id）。 */
    private Long vehicleId;

    /** 申请人 ID（sys_user.id）。 */
    private Long applicantId;

    /** 用车事由。 */
    private String purpose;

    /** 目的地。 */
    private String destination;

    /** 开始时间。 */
    private LocalDateTime startTime;

    /** 结束时间。 */
    private LocalDateTime endTime;

    /** BPM 实例 ID。 */
    private Long bpmInstanceId;

    /** 状态：APPROVING/APPROVED/REJECTED/USING/RETURNED/CANCELLED。 */
    private VehicleApplyStatus status;

    /** 实际还车时间。 */
    private LocalDateTime returnTime;

    /** 还车里程（公里）。 */
    private BigDecimal mileage;
}
