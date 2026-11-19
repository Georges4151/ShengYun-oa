package com.ceams.module.bank.enums;

/** 对账差异类型。 */
public enum DiscrepancyType {

    /** 银行有流水但系统无对应记录。 */
    MISSING_IN_SYSTEM,

    /** 系统有记录但银行无对应流水。 */
    MISSING_IN_BANK,

    /** 金额不一致。 */
    AMOUNT_MISMATCH,

    /** 日期不一致。 */
    DATE_MISMATCH,

    /** 描述不一致。 */
    DESCRIPTION_MISMATCH
}
