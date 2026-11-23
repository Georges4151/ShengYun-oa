package com.ceams.module.expense.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.ceams.module.expense.entity.ExpenseAmountRule;
import com.ceams.module.expense.enums.AmountRuleStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** 报销额度规则匹配逻辑单元测试。 */
class ExpenseAmountRuleMatcherTest {

    private static final Long PROJECT_ID = 42L;

    private static final Long DEPT_ID = 7L;

    private static final Long CHILD_DEPT_ID = 8L;

    private final ExpenseAmountRuleMatcher matcher = new ExpenseAmountRuleMatcher();

    /** 通用规则命中区间内金额。 */
    @Test
    void shouldMatchGenericRuleWithinRange() {
        ExpenseAmountRule rule = rule(1L, "通用", "0", "1000", null, null, "EXPENSE", 0);

        Optional<ExpenseAmountRule> matched =
                matcher.match(List.of(rule), new BigDecimal("500"), null, Set.of());

        assertThat(matched).contains(rule);
    }

    /** 区间边界：min 含（金额等于下限命中）。 */
    @Test
    void shouldMatchWhenAmountEqualsMinAmount() {
        ExpenseAmountRule rule = rule(1L, "通用", "100", null, null, null, "EXPENSE", 0);

        assertThat(matcher.match(List.of(rule), new BigDecimal("100"), null, Set.of()))
                .contains(rule);
    }

    /** 区间边界：max 不含（金额等于上限不命中）。 */
    @Test
    void shouldNotMatchWhenAmountEqualsMaxAmount() {
        ExpenseAmountRule rule = rule(1L, "通用", "0", "100", null, null, "EXPENSE", 0);

        assertThat(matcher.match(List.of(rule), new BigDecimal("100"), null, Set.of())).isEmpty();
        assertThat(matcher.match(List.of(rule), new BigDecimal("99.99"), null, Set.of()))
                .contains(rule);
    }

    /** max 为空表示无上限。 */
    @Test
    void shouldMatchUnboundedRuleForHugeAmount() {
        ExpenseAmountRule rule = rule(1L, "兜底", "0", null, null, null, "EXPENSE", 0);

        assertThat(matcher.match(List.of(rule), new BigDecimal("999999999.99"), null, Set.of()))
                .contains(rule);
    }

    /** 部门规则：提交人部门子树包含规则部门时命中。 */
    @Test
    void shouldMatchDeptRuleWhenDeptInSubtree() {
        ExpenseAmountRule deptRule =
                rule(1L, "部门", "0", "5000", CHILD_DEPT_ID, null, "EXPENSE_DEPT", 0);

        Optional<ExpenseAmountRule> matched =
                matcher.match(
                        List.of(deptRule),
                        new BigDecimal("1000"),
                        null,
                        Set.of(DEPT_ID, CHILD_DEPT_ID));

        assertThat(matched).contains(deptRule);
    }

    /** 部门规则：规则部门不在子树内时不命中。 */
    @Test
    void shouldNotMatchDeptRuleWhenDeptOutOfSubtree() {
        ExpenseAmountRule deptRule =
                rule(1L, "部门", "0", "5000", 99L, null, "EXPENSE_DEPT", 0);

        assertThat(
                        matcher.match(
                                List.of(deptRule),
                                new BigDecimal("1000"),
                                null,
                                Set.of(DEPT_ID, CHILD_DEPT_ID)))
                .isEmpty();
    }

    /** 维度优先级：项目规则 &gt; 部门规则 &gt; 通用规则（即使项目规则优先级更低）。 */
    @Test
    void projectRuleShouldWinOverDeptAndGeneric() {
        ExpenseAmountRule generic = rule(1L, "通用", "0", null, null, null, "EXPENSE", 100);
        ExpenseAmountRule dept = rule(2L, "部门", "0", null, DEPT_ID, null, "EXPENSE_DEPT", 50);
        ExpenseAmountRule project =
                rule(3L, "项目", "0", null, null, PROJECT_ID, "EXPENSE_PROJECT", 0);

        Optional<ExpenseAmountRule> matched =
                matcher.match(
                        List.of(generic, dept, project),
                        new BigDecimal("800"),
                        PROJECT_ID,
                        Set.of(DEPT_ID));

        assertThat(matched).contains(project);
    }

    /** 项目维度不匹配时回落到部门规则。 */
    @Test
    void shouldFallbackToDeptRuleWhenProjectNotMatched() {
        ExpenseAmountRule generic = rule(1L, "通用", "0", null, null, null, "EXPENSE", 100);
        ExpenseAmountRule dept = rule(2L, "部门", "0", null, DEPT_ID, null, "EXPENSE_DEPT", 0);

        Optional<ExpenseAmountRule> matched =
                matcher.match(
                        List.of(generic, dept), new BigDecimal("800"), 999L, Set.of(DEPT_ID));

        assertThat(matched).contains(dept);
    }

    /** 同层级排序：priority 倒序优先，其次 min_amount 倒序。 */
    @Test
    void shouldOrderByPriorityThenMinAmountDescWithinSameRank() {
        ExpenseAmountRule lowPriority = rule(1L, "低", "0", null, null, null, "EXPENSE_A", 1);
        ExpenseAmountRule highPriority = rule(2L, "高", "0", null, null, null, "EXPENSE_B", 9);
        ExpenseAmountRule samePriority =
                rule(3L, "同优先级大下限", "100", null, null, null, "EXPENSE_C", 1);

        Optional<ExpenseAmountRule> matched =
                matcher.match(
                        List.of(lowPriority, highPriority, samePriority),
                        new BigDecimal("500"),
                        null,
                        Set.of());

        assertThat(matched).contains(highPriority);

        Optional<ExpenseAmountRule> withoutHighPriority =
                matcher.match(
                        List.of(lowPriority, samePriority), new BigDecimal("500"), null, Set.of());
        assertThat(withoutHighPriority).contains(samePriority);
    }

    /** 停用规则不参与匹配。 */
    @Test
    void disabledRuleShouldBeIgnored() {
        ExpenseAmountRule disabled = rule(1L, "停用", "0", null, null, null, "EXPENSE", 0);
        disabled.setStatus(AmountRuleStatus.DISABLED);

        assertThat(matcher.match(List.of(disabled), new BigDecimal("100"), null, Set.of()))
                .isEmpty();
    }

    /** 无任何规则命中时返回空。 */
    @Test
    void shouldReturnEmptyWhenNoRuleMatched() {
        ExpenseAmountRule rule = rule(1L, "通用", "0", "100", null, null, "EXPENSE", 0);

        assertThat(matcher.match(List.of(rule), new BigDecimal("100"), null, Set.of())).isEmpty();
        assertThat(matcher.match(List.of(), new BigDecimal("50"), null, Set.of())).isEmpty();
    }

    private static ExpenseAmountRule rule(
            Long id,
            String name,
            String minAmount,
            String maxAmount,
            Long deptId,
            Long projectId,
            String templateCode,
            int priority) {
        ExpenseAmountRule rule = new ExpenseAmountRule();
        rule.setId(id);
        rule.setName(name);
        rule.setMinAmount(new BigDecimal(minAmount));
        rule.setMaxAmount(maxAmount == null ? null : new BigDecimal(maxAmount));
        rule.setDeptId(deptId);
        rule.setProjectId(projectId);
        rule.setTemplateCode(templateCode);
        rule.setPriority(priority);
        rule.setStatus(AmountRuleStatus.ENABLED);
        return rule;
    }
}
