package com.ceams.module.bank.enums;

/** 银行流水对账状态。 */
public enum BankStatementStatus {

    /** 未对账。 */
    UNRECONCILED,

    /** 已对账（存在差异，已生成对账结果）。 */
    RECONCILED,

    /** 完全匹配。 */
    MATCHED
}
