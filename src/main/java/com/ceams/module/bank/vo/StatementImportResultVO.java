package com.ceams.module.bank.vo;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/** 银行流水导入结果。 */
@Data
public class StatementImportResultVO {

    /** 解析出的总行数（不含表头）。 */
    private int total;

    /** 成功落库数。 */
    private int success;

    /** 因重复（account_id + bank_reference）跳过的行数。 */
    private int skipped;

    /** 因校验/解析失败而未落库的行数。 */
    private int failed;

    /** 失败明细（行号 + 原因）。 */
    private List<ErrorItem> errors = new ArrayList<>();

    /** 单行失败明细。 */
    @Data
    public static class ErrorItem {

        /** 行号（表头为第 1 行，数据行从 2 开始）。 */
        private int row;

        /** 失败原因。 */
        private String reason;

        public ErrorItem() {}

        public ErrorItem(int row, String reason) {
            this.row = row;
            this.reason = reason;
        }
    }
}
