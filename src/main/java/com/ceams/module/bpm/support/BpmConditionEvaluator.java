package com.ceams.module.bpm.support;

import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import java.math.BigDecimal;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 条件求值器（仅服务端简单比较，不支持脚本）。
 *
 * <p>支持：
 *
 * <ul>
 *   <li>表单字段数值比较：{@code EQ/NE/GT/GE/LT/LE}（兼容 {@code == != > >= < <=}）；
 *   <li>字符串相等：{@code EQ/NE}（两侧均可解析为数值时按数值比较）；
 *   <li>角色匹配：字段 {@code initiatorRole/role}，比较角色编码（忽略大小写与 ROLE_ 前缀）；
 *   <li>部门匹配：字段 {@code initiatorDept/dept}，比较部门 ID 或部门名称。
 * </ul>
 *
 * <p>变量缺失或组织数据缺失时返回 {@code false} 并记录告警；运算符不支持视为模板配置错误，
 * 抛出 {@link ErrorCode#BPM_SCHEMA_INVALID}，避免静默走错分支。
 */
@Slf4j
@Component
public class BpmConditionEvaluator {

    private static final String OP_EQ = "EQ";
    private static final String OP_NE = "NE";
    private static final String OP_GT = "GT";
    private static final String OP_GE = "GE";
    private static final String OP_LT = "LT";
    private static final String OP_LE = "LE";

    /** 判断条件是否命中；无法求值时降级为不命中。 */
    public boolean matches(BpmSchemaCondition condition, BpmConditionContext context) {
        if (condition == null || context == null) {
            return false;
        }
        String operator = normalizeOperator(condition.getOperator());
        if (operator == null) {
            throw BusinessException.of(
                    ErrorCode.BPM_SCHEMA_INVALID, "不支持的条件运算符：" + condition.getOperator());
        }
        String field = condition.getField() == null ? "" : condition.getField().trim();
        String normalizedField = field.toLowerCase(Locale.ROOT);
        String expected = condition.getValue() == null ? "" : condition.getValue().trim();

        if (isRoleField(normalizedField)) {
            boolean hit = roleHit(context.getRoles(), expected);
            return applyLogical(hit, operator, field, expected);
        }
        if (isDeptField(normalizedField)) {
            if (context.getInitiatorDeptIds().isEmpty()
                    && context.getInitiatorDeptNames().isEmpty()) {
                log.warn(
                        "条件求值降级为不命中：发起人部门数据缺失，field={}，value={}",
                        field,
                        expected);
                return false;
            }
            boolean hit = deptHit(context, expected);
            return applyLogical(hit, operator, field, expected);
        }

        String actual = resolveVariable(context, field);
        if (actual == null) {
            log.warn("条件求值降级为不命中：流程变量缺失，instance 变量中无 field={}", field);
            return false;
        }
        return compare(actual, expected, operator, field);
    }

    // ------------------------------------------------------------------
    // 角色 / 部门匹配
    // ------------------------------------------------------------------

    private boolean isRoleField(String normalizedField) {
        return "initiatorrole".equals(normalizedField) || "role".equals(normalizedField);
    }

    private boolean isDeptField(String normalizedField) {
        return "initiatordept".equals(normalizedField)
                || "initiatordeptid".equals(normalizedField)
                || "dept".equals(normalizedField);
    }

    private boolean roleHit(Set<String> roles, String expected) {
        if (roles == null || roles.isEmpty() || expected.isEmpty()) {
            return false;
        }
        String target = normalizeRole(expected);
        return roles.stream()
                .filter(Objects::nonNull)
                .map(this::normalizeRole)
                .anyMatch(target::equals);
    }

    private String normalizeRole(String raw) {
        String value = raw.trim().toUpperCase(Locale.ROOT);
        return value.startsWith("ROLE_") ? value.substring(5) : value;
    }

    private boolean deptHit(BpmConditionContext context, String expected) {
        Long expectedId = parseLong(expected);
        if (expectedId != null && context.getInitiatorDeptIds().contains(expectedId)) {
            return true;
        }
        for (String name : context.getInitiatorDeptNames()) {
            if (name != null && name.trim().equalsIgnoreCase(expected)) {
                return true;
            }
        }
        return false;
    }

    /** 角色/部门为布尔命中，只支持 EQ/NE；其他运算符视为配置错误，降级不命中。 */
    private boolean applyLogical(
            boolean hit, String operator, String field, String expected) {
        if (OP_EQ.equals(operator)) {
            return hit;
        }
        if (OP_NE.equals(operator)) {
            return !hit;
        }
        log.warn(
                "条件求值降级为不命中：角色/部门条件仅支持 EQ/NE，field={}，operator={}，value={}",
                field,
                operator,
                expected);
        return false;
    }

    // ------------------------------------------------------------------
    // 变量比较
    // ------------------------------------------------------------------

    /** 变量取值：优先精确匹配，其次忽略大小写匹配。 */
    private String resolveVariable(BpmConditionContext context, String field) {
        if (context.getVariables() == null) {
            return null;
        }
        String actual = context.getVariables().get(field);
        if (actual != null) {
            return actual;
        }
        for (Map.Entry<String, String> entry : context.getVariables().entrySet()) {
            if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(field)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private boolean compare(String actual, String expected, String operator, String field) {
        if (OP_EQ.equals(operator) || OP_NE.equals(operator)) {
            boolean equal = valuesEqual(actual, expected);
            return OP_EQ.equals(operator) == equal;
        }
        BigDecimal actualNumber = parseNumber(actual);
        BigDecimal expectedNumber = parseNumber(expected);
        if (actualNumber == null || expectedNumber == null) {
            log.warn(
                    "条件求值降级为不命中：非数值无法比较，field={}，actual={}，value={}，operator={}",
                    field,
                    actual,
                    expected,
                    operator);
            return false;
        }
        int cmp = actualNumber.compareTo(expectedNumber);
        return switch (operator) {
            case OP_GT -> cmp > 0;
            case OP_GE -> cmp >= 0;
            case OP_LT -> cmp < 0;
            case OP_LE -> cmp <= 0;
            default -> false;
        };
    }

    private boolean valuesEqual(String actual, String expected) {
        BigDecimal actualNumber = parseNumber(actual);
        BigDecimal expectedNumber = parseNumber(expected);
        if (actualNumber != null && expectedNumber != null) {
            return actualNumber.compareTo(expectedNumber) == 0;
        }
        return actual.trim().equals(expected.trim());
    }

    private BigDecimal parseNumber(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** 运算符归一化：兼容符号写法；未知运算符返回 null。 */
    private String normalizeOperator(String raw) {
        if (raw == null) {
            return null;
        }
        String op = raw.trim().toUpperCase(Locale.ROOT);
        return switch (op) {
            case "EQ", "==", "=" -> OP_EQ;
            case "NE", "!=", "<>" -> OP_NE;
            case "GT", ">" -> OP_GT;
            case "GE", ">=" -> OP_GE;
            case "LT", "<" -> OP_LT;
            case "LE", "<=" -> OP_LE;
            default -> null;
        };
    }
}
