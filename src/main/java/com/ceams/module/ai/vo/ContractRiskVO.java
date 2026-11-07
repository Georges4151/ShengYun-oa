package com.ceams.module.ai.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 合同风险提示（单条规则命中结果）。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContractRiskVO {

    /** 风险级别：HIGH / MEDIUM / LOW。 */
    private String level;

    /** 规则编码，如 OVERPAID、EXPIRED_ACTIVE。 */
    private String code;

    /** 面向业务人员的中文标题。 */
    private String title;

    /** 中文明细说明。 */
    private String detail;
}
