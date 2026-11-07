package com.ceams.module.ai.tool;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 工具 JSON Schema 构造辅助，保证各只读工具的 schema 结构一致。 */
public final class ToolSchemas {

    private ToolSchemas() {}

    /** 构造 {@code type=object} 的参数 schema。 */
    public static Map<String, Object> object(Map<String, Object> properties, String... required) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", List.of(required));
        return schema;
    }

    /** 字符串参数。 */
    public static Map<String, Object> text(String description) {
        Map<String, Object> property = new LinkedHashMap<>();
        property.put("type", "string");
        property.put("description", description);
        return property;
    }

    /** 整数参数（含边界）。 */
    public static Map<String, Object> integer(String description, int minimum, int maximum) {
        Map<String, Object> property = new LinkedHashMap<>();
        property.put("type", "integer");
        property.put("description", description);
        property.put("minimum", minimum);
        property.put("maximum", maximum);
        return property;
    }

    /** 统计期间参数（统一描述，避免模型把「本月」猜成错误年份）。 */
    public static Map<String, Object> period() {
        return text(
                "统计期间：yyyy-MM 或 yyyy-MM-dd~yyyy-MM-dd；不填默认本月；"
                        + "用户说「本月/上月」时必须换算成具体年月（以系统提示中的当前月份为准），不要猜年份");
    }

    /** 枚举字符串参数。 */
    public static Map<String, Object> choice(String description, String... values) {
        Map<String, Object> property = text(description);
        property.put("enum", List.of(values));
        return property;
    }
}
