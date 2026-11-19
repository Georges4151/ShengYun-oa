package com.ceams.module.admin.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ceams.common.base.BaseEntity;
import com.ceams.module.admin.enums.VehicleExpenseType;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 行政车辆费用（admin_vehicle_expense）。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("admin_vehicle_expense")
public class AdminVehicleExpense extends BaseEntity {

    /** 车辆 ID（逻辑引用 admin_vehicle.id）。 */
    private Long vehicleId;

    /** 关联用车单 ID，可空。 */
    private Long applyId;

    /** 费用类型：MAINTENANCE/VIOLATION/FUEL。 */
    private VehicleExpenseType expenseType;

    /** 金额。 */
    private BigDecimal amount;

    /** 发生日期。 */
    private LocalDate occurDate;

    /** 附件（file 模块服务端文件名）。 */
    private String attachmentName;

    /** 备注。 */
    private String remark;
}
