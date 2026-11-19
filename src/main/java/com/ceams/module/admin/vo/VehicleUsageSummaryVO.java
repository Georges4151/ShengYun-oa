package com.ceams.module.admin.vo;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/** 用车汇总（只读聚合视图）。 */
@Data
public class VehicleUsageSummaryVO {

    /** 区间内用车申请总数。 */
    private long applyCount;

    /** 区间内审批通过及之后状态（APPROVED/USING/RETURNED）的申请数。 */
    private long approvedCount;

    /** 区间内还车里程合计（公里）。 */
    private BigDecimal totalMileage;

    /** 区间内车辆费用合计。 */
    private BigDecimal totalExpense;

    /** 按车辆分组的明细，默认空列表。 */
    private List<VehicleUsageItemVO> items = new ArrayList<>();
}
