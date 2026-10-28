package com.ceams.module.bank.vo;

import lombok.Data;

/**
 * 自动对账结果统计。
 */
@Data
public class ReconciliationMatchResultVO {

    /** 完全匹配数。 */
    private int matched;

    /** 无对应流水数（银行有系统无 + 系统有银行无）。 */
    private int unmatched;

    /** 部分匹配数（存在差异）。 */
    private int partialMatched;

    public ReconciliationMatchResultVO() {
    }

    public ReconciliationMatchResultVO(int matched, int unmatched, int partialMatched) {
        this.matched = matched;
        this.unmatched = unmatched;
        this.partialMatched = partialMatched;
    }
}
