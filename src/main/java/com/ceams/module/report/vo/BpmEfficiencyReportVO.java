package com.ceams.module.report.vo;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/** 审批效率报表视图对象：实例量、通过量、平均时长、超时率与维度明细。 */
@Data
public class BpmEfficiencyReportVO {

    /** 流程实例数。 */
    private long instanceCount;

    /** 已通过实例数（status = APPROVED）。 */
    private long approvedCount;

    /** 平均审批时长（小时，仅已通过实例，无数据取 0）。 */
    private BigDecimal avgDurationHours = BigDecimal.ZERO;

    /** 超时任务数（PENDING 且 due_at 早于当前时间）。 */
    private long overdueTaskCount;

    /** 超时率（overdueTaskCount / 任务总数，保留 4 位小数）。 */
    private BigDecimal overdueRate = BigDecimal.ZERO;

    /** 按业务类型统计（bizType 升序）。 */
    private List<BpmBizTypeStatVO> byBizType = new ArrayList<>();

    /** 待办积压节点（pendingCount 倒序）。 */
    private List<BpmNodeBacklogVO> backlogByNode = new ArrayList<>();
}
