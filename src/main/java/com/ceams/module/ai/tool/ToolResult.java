package com.ceams.module.ai.tool;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 只读工具执行结果。
 *
 * @param rows 模型可见的结果行（最多 {@code ceams.ai.max-tool-rows} 行）
 * @param totalRows 命中总行数（可能大于 {@code rows.size()}）
 * @param truncated 是否因行数上限被截断
 */
public record ToolResult(List<Map<String, Object>> rows, int totalRows, boolean truncated) {

    public ToolResult {
        rows = rows == null ? Collections.emptyList() : List.copyOf(rows);
    }

    /** 空结果。 */
    public static ToolResult empty() {
        return new ToolResult(Collections.emptyList(), 0, false);
    }

    /** 无权限或不可用时的单行说明结果。 */
    public static ToolResult denied(String reason) {
        return new ToolResult(List.of(Map.of("permitted", false, "reason", reason)), 1, false);
    }

    /** 按最大行数截断；行数由调用方负责构造。 */
    public static ToolResult capped(List<Map<String, Object>> rows, int totalRows, int maxRows) {
        return capped(rows, totalRows, false, maxRows);
    }

    /** 按最大行数截断，并允许调用方显式标记截断（如 Top 分类被裁剪）。 */
    public static ToolResult capped(
            List<Map<String, Object>> rows, int totalRows, boolean truncated, int maxRows) {
        List<Map<String, Object>> safeRows = rows == null ? Collections.emptyList() : rows;
        int safeMax = Math.max(1, maxRows);
        if (safeRows.size() <= safeMax) {
            return new ToolResult(safeRows, Math.max(totalRows, safeRows.size()), truncated);
        }
        List<Map<String, Object>> limited = new ArrayList<>(safeRows.subList(0, safeMax));
        return new ToolResult(limited, Math.max(totalRows, safeRows.size()), true);
    }
}
