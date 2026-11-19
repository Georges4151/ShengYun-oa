package com.ceams.module.ai.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 报销预审风险提示（单条规则命中结果）。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PreAuditWarningVO {

    /** 规则编码，如 AMOUNT_THRESHOLD、DUPLICATE_NUMBER。 */
    private String rule;

    /** 风险级别：HIGH / MEDIUM。 */
    private String level;

    /** 面向审批人的中文提示文案。 */
    private String message;
}
