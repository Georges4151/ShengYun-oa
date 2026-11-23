package com.ceams.module.expense.enums;

/**
 * 报销额度规则状态（biz_expense_amount_rule.status）。
 *
 * <p>枚举 name 即 varchar 代码值：ENABLED 参与提交匹配，DISABLED 仅保留配置。
 */
public enum AmountRuleStatus {
    /** 启用：参与报销提交时的流程模板匹配。 */
    ENABLED,
    /** 停用：不参与匹配，规则配置保留。 */
    DISABLED
}
