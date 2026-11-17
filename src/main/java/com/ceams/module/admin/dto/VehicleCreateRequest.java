package com.ceams.module.admin.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;
import lombok.Data;

/** 车辆建档请求。 */
@Data
public class VehicleCreateRequest {

    /** 车牌号，唯一。 */
    @NotBlank(message = "车牌号不能为空")
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

    /** 备注。 */
    private String remark;
}
