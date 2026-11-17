package com.ceams.module.report.vo;

import java.math.BigDecimal;
import lombok.Data;

/** 审批效率报表：单业务类型统计行。 */
@Data
public class BpmBizTypeStatVO {

    /** 业务类型（bpm_instance.biz_type）。 */
    private String bizType;

    /** 实例数。 */
    private long instanceCount;

    /** 已通过实例数。 */
    private long approvedCount;

    /** 平均审批时长（小时，仅已通过实例，无数据取 0）。 */
    private BigDecimal avgDurationHours = BigDecimal.ZERO;
}
