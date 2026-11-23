package com.ceams.module.expense.vo;

import com.ceams.module.expense.enums.AmountRuleStatus;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 报销额度分级规则视图对象。
 *
 * <p>所有 Long / ID 字段统一序列化为字符串，避免雪花 ID 超出 JS 安全整数范围。
 */
@Data
public class AmountRuleVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 规则名称。 */
    private String name;

    /** 金额区间下限（含）。 */
    private BigDecimal minAmount;

    /** 金额区间上限（不含），为空表示无上限。 */
    private BigDecimal maxAmount;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long deptId;

    /** 部门名称（冗余展示）。 */
    private String deptName;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long projectId;

    /** 项目名称（冗余展示）。 */
    private String projectName;

    /** BPM 流程模板编码。 */
    private String templateCode;

    /** BPM 流程模板名称（冗余展示）。 */
    private String templateName;

    /** 优先级。 */
    private Integer priority;

    /** 状态。 */
    private AmountRuleStatus status;

    /** 备注。 */
    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
