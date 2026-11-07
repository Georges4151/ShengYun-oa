package com.ceams.module.ai.tool;

import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import java.util.Locale;
import java.util.Map;

/** 工具参数解析：宽松接受数字/字符串，非法值统一转为参数错误而非 500。 */
public final class ToolArgs {

    /** 全局工具结果行数硬上限（与方案 §6 一致）。 */
    public static final int MAX_TOOL_ROWS = 200;

    /** 默认返回行数。 */
    public static final int DEFAULT_LIMIT = 20;

    private ToolArgs() {}

    /** 读取去空白后的字符串参数，空串视为未传。 */
    public static String text(Map<String, Object> args, String key) {
        if (args == null) {
            return null;
        }
        Object value = args.get(key);
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    /** 读取 Long 参数：支持数字与数字字符串。 */
    public static Long longValue(Map<String, Object> args, String key) {
        Object value = args == null ? null : args.get(key);
        if (value == null) {
            return null;
        }
        return parseLong(value, key);
    }

    /** 读取枚举参数（忽略大小写），非法值抛参数错误。 */
    public static <E extends Enum<E>> E enumValue(
            Map<String, Object> args, String key, Class<E> type) {
        String text = text(args, key);
        if (text == null) {
            return null;
        }
        try {
            return Enum.valueOf(type, text.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw BusinessException.of(
                    ErrorCode.VALID_ILLEGAL_ARGUMENT, "参数 " + key + " 非法：" + text);
        }
    }

    /** 读取 limit：默认 20，上限为配置值与全局 200 行中的较小者。 */
    public static int limit(Map<String, Object> args, int configuredMaxRows) {
        Object value = args == null ? null : args.get("limit");
        long raw = DEFAULT_LIMIT;
        if (value != null) {
            raw = parseLong(value, "limit");
            if (raw <= 0) {
                throw BusinessException.of(ErrorCode.VALID_ILLEGAL_ARGUMENT, "参数 limit 必须大于 0");
            }
        }
        int max = Math.min(Math.max(1, configuredMaxRows), MAX_TOOL_ROWS);
        return (int) Math.min(raw, max);
    }

    private static long parseLong(Object value, String key) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        String text = String.valueOf(value).trim();
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException ex) {
            throw BusinessException.of(
                    ErrorCode.VALID_ILLEGAL_ARGUMENT, "参数 " + key + " 非法：" + text);
        }
    }
}
