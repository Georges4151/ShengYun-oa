package com.ceams.module.report.vo;

import java.math.BigDecimal;
import lombok.Data;

/** 审批效率业务类型聚合行（Mapper 内部使用）。 */
@Data
public class BpmBizTypeRowVO {

    /** 业务类型。 */
    private String bizType;

    /** 实例数。 */
    private Long instanceCount;

    /** 已通过实例数。 */
    private Long approvedCount;

    /** 已通过实例的平均时长（秒）。 */
    private BigDecimal avgDurationSeconds;
}
