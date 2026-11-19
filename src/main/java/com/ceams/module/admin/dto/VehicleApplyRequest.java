package com.ceams.module.admin.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.Data;

/** 用车申请请求。 */
@Data
public class VehicleApplyRequest {

    /** 车辆 ID。 */
    @NotNull(message = "车辆 ID 不能为空")
    private Long vehicleId;

    /** 用车事由。 */
    private String purpose;

    /** 目的地。 */
    private String destination;

    /** 开始时间。 */
    @NotNull(message = "开始时间不能为空")
    private LocalDateTime startTime;

    /** 结束时间。 */
    @NotNull(message = "结束时间不能为空")
    private LocalDateTime endTime;
}
