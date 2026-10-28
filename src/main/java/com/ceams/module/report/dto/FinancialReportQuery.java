package com.ceams.module.report.dto;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 财务报表查询参数（全部可选，按需过滤）。
 */
@Data
public class FinancialReportQuery {

    /** 月份（YYYY-MM），过滤交易时间所在月。 */
    @Pattern(regexp = "^\\d{4}-(0[1-9]|1[0-2])$", message = "月份格式必须为 YYYY-MM")
    private String month;

    /** 项目 ID（逻辑引用 biz_project.id）。 */
    private Long projectId;

    /** 账户 ID（逻辑引用 biz_payment_account.id）。 */
    private Long accountId;
}
