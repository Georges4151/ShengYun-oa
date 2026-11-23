package com.ceams.module.expense.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ceams.common.base.BaseEntity;
import com.ceams.module.expense.enums.AmountRuleStatus;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 报销额度分级规则（biz_expense_amount_rule）。
 *
 * <p>维度 = 金额区间（min 含 / max 不含，max 为空表示无上限）+ 可选部门 + 可选项目，
 * 映射到一个 BPM 流程模板编码；部门匹配含子部门，未命中的金额不允许提交报销。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_expense_amount_rule")
public class ExpenseAmountRule extends BaseEntity {

    /** 规则名称。 */
    private String name;

    /** 金额区间下限（含）。 */
    private BigDecimal minAmount;

    /** 金额区间上限（不含），为空表示无上限。 */
    private BigDecimal maxAmount;

    /** 适用部门 ID（逻辑引用 sys_dept.id，匹配时含子部门；为空表示不限部门）。 */
    private Long deptId;

    /** 适用项目 ID（逻辑引用 biz_project.id；为空表示不限项目）。 */
    private Long projectId;

    /** 映射的 BPM 流程模板编码（逻辑引用 bpm_template.code）。 */
    private String templateCode;

    /** 优先级（同层级内数值越大越优先）。 */
    private Integer priority;

    /** 状态。 */
    private AmountRuleStatus status = AmountRuleStatus.ENABLED;

    /** 备注。 */
    private String remark;
}
