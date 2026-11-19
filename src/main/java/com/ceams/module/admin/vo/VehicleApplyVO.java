package com.ceams.module.admin.vo;

import com.ceams.module.admin.enums.VehicleApplyStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/** 用车申请展示对象。 */
@Data
public class VehicleApplyVO {

    /** 申请单 ID。 */
    private Long id;

    /** 车辆 ID。 */
    private Long vehicleId;

    /** 车牌号。 */
    private String vehiclePlateNo;

    /** 申请人 ID。 */
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

    /** 状态。 */
    private VehicleApplyStatus status;

    /** 实际还车时间。 */
    private LocalDateTime returnTime;

    /** 还车里程。 */
    private BigDecimal mileage;

    /** 创建时间。 */
    private LocalDateTime createTime;
}
