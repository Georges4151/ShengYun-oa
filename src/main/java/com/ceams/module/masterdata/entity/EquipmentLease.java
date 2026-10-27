package com.ceams.module.masterdata.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ceams.common.base.BaseEntity;
import com.ceams.module.masterdata.enums.PaymentStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 设备租赁（biz_equipment_lease）。
 *
 * <p>project_id 仅作 Long 逻辑引用，不依赖 project 域代码。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_equipment_lease")
public class EquipmentLease extends BaseEntity {

    /** 设备 ID（逻辑引用 biz_equipment.id）。 */
    private Long equipmentId;

    /** 项目 ID（逻辑引用）。 */
    private Long projectId;

    /** 出租方 ID（逻辑引用 biz_contact.id）。 */
    private Long lessorId;

    /** 租赁开始日期。 */
    private LocalDateTime leaseStartDate;

    /** 租赁结束日期。 */
    private LocalDateTime leaseEndDate;

    /** 日租金。 */
    private BigDecimal dailyRent;

    /** 总租金。 */
    private BigDecimal totalRent;

    /** 付款状态。 */
    private PaymentStatus paymentStatus;
}