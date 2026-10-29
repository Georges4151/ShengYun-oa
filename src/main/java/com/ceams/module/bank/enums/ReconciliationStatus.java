package com.ceams.module.bank.enums;

/** 对账结果状态。 */
public enum ReconciliationStatus {

    /** 完全匹配。 */
    MATCHED,

    /** 无对应流水（银行有系统无 / 系统有银行无）。 */
    UNMATCHED,

    /** 部分匹配（金额/日期/描述等存在差异）。 */
    PARTIAL_MATCHED
}
