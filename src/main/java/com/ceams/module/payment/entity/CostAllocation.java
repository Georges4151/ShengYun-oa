package com.ceams.module.payment.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ceams.common.base.BaseEntity;
import com.ceams.module.payment.enums.TargetType;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 成本分摊（biz_cost_allocation）。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_cost_allocation")
public class CostAllocation extends BaseEntity {

    /** 账户记录ID（逻辑引用 biz_account.id）。 */
    private Long accountRecordId;

    /** 分摊目标类型：PROJECT/DEPT/SERVICE/CONTRACT。 */
    private TargetType targetType;

    /** 分摊目标ID（逻辑引用）。 */
    private Long targetId;

    /** 分摊金额。 */
    private BigDecimal allocationAmount;

    /** 分摊比例。 */
    private BigDecimal allocationRatio;
}
