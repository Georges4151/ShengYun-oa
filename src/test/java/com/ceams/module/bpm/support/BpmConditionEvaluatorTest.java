package com.ceams.module.bpm.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** 条件求值器单元测试：数值比较、字符串相等、角色/部门匹配与降级。 */
class BpmConditionEvaluatorTest {

    private BpmConditionEvaluator evaluator;

    @BeforeEach
    void setUp() {
        evaluator = new BpmConditionEvaluator();
    }

    @Test
    void 金额大于等于阈值命中() {
        assertThat(evaluator.matches(condition("amount", "GE", "5000"), context(Map.of("amount", "8000"))))
                .isTrue();
    }

    @Test
    void 金额小于阈值不命中() {
        assertThat(evaluator.matches(condition("amount", "GE", "5000"), context(Map.of("amount", "3000"))))
                .isFalse();
    }

    @Test
    void 数值相等忽略小数格式差异() {
        assertThat(evaluator.matches(condition("amount", "==", "5000"), context(Map.of("amount", "5000.00"))))
                .isTrue();
    }

    @Test
    void 非数值字段做大小比较时降级不命中() {
        assertThat(evaluator.matches(condition("level", "GT", "3"), context(Map.of("level", "高"))))
                .isFalse();
    }

    @Test
    void 变量缺失时降级不命中() {
        assertThat(evaluator.matches(condition("amount", "GE", "5000"), context(Map.of())))
                .isFalse();
    }

    @Test
    void 角色条件忽略大小写与ROLE前缀() {
        BpmConditionContext context = context(Map.of());
        context.setRoles(Set.of("ROLE_FINANCE"));
        assertThat(evaluator.matches(condition("initiatorRole", "EQ", "finance"), context)).isTrue();
        assertThat(evaluator.matches(condition("role", "NE", "HR"), context)).isTrue();
    }

    @Test
    void 部门条件按部门ID命中() {
        BpmConditionContext context = context(Map.of());
        context.setInitiatorDeptIds(Set.of(3L, 4L));
        assertThat(evaluator.matches(condition("initiatorDept", "EQ", "3"), context)).isTrue();
        assertThat(evaluator.matches(condition("dept", "EQ", "5"), context)).isFalse();
    }

    @Test
    void 部门条件按部门名命中() {
        BpmConditionContext context = context(Map.of());
        context.setInitiatorDeptNames(Set.of("销售部"));
        assertThat(evaluator.matches(condition("dept", "EQ", "销售部"), context)).isTrue();
    }

    @Test
    void 部门数据缺失时降级不命中() {
        BpmConditionContext context = context(Map.of());
        assertThat(evaluator.matches(condition("dept", "EQ", "3"), context)).isFalse();
        assertThat(evaluator.matches(condition("dept", "NE", "3"), context)).isFalse();
    }

    @Test
    void 不支持的运算符抛出模板错误() {
        assertThatThrownBy(
                        () -> evaluator.matches(condition("amount", "LIKE", "5000"), context(Map.of("amount", "8000"))))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.BPM_SCHEMA_INVALID);
    }

    @Test
    void 符号运算符归一化后正常求值() {
        assertThat(evaluator.matches(condition("amount", ">", "5000"), context(Map.of("amount", "8000"))))
                .isTrue();
        assertThat(evaluator.matches(condition("amount", "<=", "5000"), context(Map.of("amount", "5000"))))
                .isTrue();
        assertThat(evaluator.matches(condition("amount", "!=", "5000.00"), context(Map.of("amount", "6000"))))
                .isTrue();
    }

    // ------------------------------------------------------------------
    // 夹具
    // ------------------------------------------------------------------

    private BpmSchemaCondition condition(String field, String operator, String value) {
        BpmSchemaCondition condition = new BpmSchemaCondition();
        condition.setField(field);
        condition.setOperator(operator);
        condition.setValue(value);
        condition.setTargetNodeKey("target");
        return condition;
    }

    private BpmConditionContext context(Map<String, String> variables) {
        BpmConditionContext context = new BpmConditionContext();
        context.setVariables(variables);
        return context;
    }
}
