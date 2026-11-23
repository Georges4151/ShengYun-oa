package com.ceams.module.expense.support;

import com.ceams.module.expense.entity.ExpenseAmountRule;
import com.ceams.module.expense.enums.AmountRuleStatus;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * 报销额度规则匹配器（纯内存计算，不访问数据库）。
 *
 * <p>匹配口径：
 *
 * <ol>
 *   <li>仅 ENABLED 规则参与匹配；
 *   <li>金额区间语义 min 含、max 不含，max 为空表示无上限；
 *   <li>维度优先级：项目规则 &gt; 部门规则（含子部门）&gt; 通用规则；
 *   <li>同维度按 priority 倒序、min_amount 倒序，仍相同则按 id 升序保证结果稳定。
 * </ol>
 */
@Component
public class ExpenseAmountRuleMatcher {

    /** 维度层级：项目规则。 */
    private static final int RANK_PROJECT = 1;

    /** 维度层级：部门规则。 */
    private static final int RANK_DEPT = 2;

    /** 维度层级：通用规则。 */
    private static final int RANK_GENERIC = 3;

    /**
     * 从候选规则中选出命中的最优规则。
     *
     * @param rules 候选规则（可包含停用规则，内部会过滤）
     * @param amount 报销金额
     * @param projectId 报销归属项目 ID（可空）
     * @param deptSubtreeIds 提交人所属部门子树 ID 集合（含自身，可空）
     * @return 命中的最优规则；无命中返回 {@link Optional#empty()}
     */
    public Optional<ExpenseAmountRule> match(
            List<ExpenseAmountRule> rules,
            BigDecimal amount,
            Long projectId,
            Set<Long> deptSubtreeIds) {
        if (rules == null || rules.isEmpty() || amount == null) {
            return Optional.empty();
        }
        Set<Long> deptIds = deptSubtreeIds == null ? Set.of() : deptSubtreeIds;
        return rules.stream()
                .filter(Objects::nonNull)
                .filter(rule -> rule.getStatus() != AmountRuleStatus.DISABLED)
                .filter(rule -> amountMatches(rule, amount))
                .filter(rule -> dimensionMatches(rule, projectId, deptIds))
                .min(bestFirstComparator());
    }

    /** 金额区间判定：min 含、max 不含，max 为空表示无上限。 */
    boolean amountMatches(ExpenseAmountRule rule, BigDecimal amount) {
        if (rule.getMinAmount() == null || amount.compareTo(rule.getMinAmount()) < 0) {
            return false;
        }
        return rule.getMaxAmount() == null || amount.compareTo(rule.getMaxAmount()) < 0;
    }

    /**
     * 维度判定：项目规则要求项目一致；部门规则要求命中所属部门子树；
     * 同时配置部门与项目的规则需两者都命中。
     */
    boolean dimensionMatches(ExpenseAmountRule rule, Long projectId, Set<Long> deptSubtreeIds) {
        if (rule.getProjectId() != null && !Objects.equals(rule.getProjectId(), projectId)) {
            return false;
        }
        return rule.getDeptId() == null || deptSubtreeIds.contains(rule.getDeptId());
    }

    /** 维度层级：项目规则 &gt; 部门规则 &gt; 通用规则。 */
    private static int dimensionRank(ExpenseAmountRule rule) {
        if (rule.getProjectId() != null) {
            return RANK_PROJECT;
        }
        return rule.getDeptId() != null ? RANK_DEPT : RANK_GENERIC;
    }

    /** 排序：层级升序 → 优先级降序 → 金额下限降序 → id 升序。 */
    private static Comparator<ExpenseAmountRule> bestFirstComparator() {
        return Comparator.comparingInt(ExpenseAmountRuleMatcher::dimensionRank)
                .thenComparing(
                        Comparator.comparingInt(ExpenseAmountRuleMatcher::priorityOf).reversed())
                .thenComparing(
                        ExpenseAmountRuleMatcher::minAmountOf,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(
                        ExpenseAmountRuleMatcher::idOf,
                        Comparator.nullsLast(Comparator.naturalOrder()));
    }

    private static int priorityOf(ExpenseAmountRule rule) {
        return rule.getPriority() == null ? 0 : rule.getPriority();
    }

    private static BigDecimal minAmountOf(ExpenseAmountRule rule) {
        return rule.getMinAmount();
    }

    private static Long idOf(ExpenseAmountRule rule) {
        return rule.getId();
    }
}
