package com.ceams.module.bank.importer;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

/** 银行流水导入中间行：解析器产出的统一结构，后续由导入器做校验、去重与落库。 */
@Data
public class StatementRow {

    /** 账户标识（导入时由外部传入或由表头映射）。 */
    private String accountId;

    /** 交易日期。 */
    private LocalDate transactionDate;

    /** 交易金额。 */
    private BigDecimal amount;

    /** 收支类型 / 借贷方向。 */
    private String type;

    /** 摘要描述。 */
    private String description;

    /** 对方户名。 */
    private String counterparty;

    /** 银行流水号 / 交易参考号（去重键之一）。 */
    private String bankReference;

    /** 交易后余额。 */
    private BigDecimal balance;
}
