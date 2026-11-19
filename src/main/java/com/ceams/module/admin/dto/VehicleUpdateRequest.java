package com.ceams.module.admin.dto;

import java.time.LocalDate;
import lombok.Data;

/** 车辆编辑请求（状态只由审批回调写入，不在此调整）。 */
@Data
public class VehicleUpdateRequest {

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

    /** 备注。 */
    private String remark;
}
