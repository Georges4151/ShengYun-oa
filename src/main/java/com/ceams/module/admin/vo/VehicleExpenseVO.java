package com.ceams.module.admin.vo;

import com.ceams.module.admin.enums.VehicleExpenseType;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

/** 车辆费用展示对象。 */
@Data
public class VehicleExpenseVO {

    /** 费用 ID。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 车辆 ID。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long vehicleId;

    /** 关联用车单 ID。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long applyId;

    /** 费用类型。 */
    private VehicleExpenseType expenseType;

    /** 金额。 */
    private BigDecimal amount;

    /** 发生日期。 */
    private LocalDate occurDate;

    /** 附件文件名。 */
    private String attachmentName;

    /** 备注。 */
    private String remark;

    /** 创建时间。 */
    private LocalDateTime createTime;
}
