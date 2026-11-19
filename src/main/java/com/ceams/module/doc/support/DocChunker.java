package com.ceams.module.doc.support;

import java.util.ArrayList;
import java.util.List;

/**
 * 文本分块工具：按目标长度切分并在后半段优先按标点断句，相邻分块保留重叠。
 *
 * <p>约束：
 *
 * <ul>
 *   <li>不丢内容：相邻分块重叠区不影响覆盖，除纯空白外原文全部进入某个分块；
 *   <li>不无限循环：每次推进量至少为 1，且不超过窗口末尾；
 *   <li>空白文本返回空列表，分块结果丢弃纯空白片段。
 * </ul>
 */
public final class DocChunker {

    /** 默认分块长度（字符）。 */
    public static final int DEFAULT_SIZE = 600;

    /** 默认重叠长度（字符）。 */
    public static final int DEFAULT_OVERLAP = 100;

    /** 断句字符：换行与中英文常用句末标点。 */
    private static final String BREAK_CHARS = "\n。；;！？!?";

    private DocChunker() {}

    /** 按默认长度 / 重叠切分。 */
    public static List<String> split(String text) {
        return split(text, DEFAULT_SIZE, DEFAULT_OVERLAP);
    }

    /**
     * 切分文本。
     *
     * @param text 原文（可含换行，调用方负责已完成归一化）
     * @param size 目标分块长度（<=0 时取默认值）
     * @param overlap 相邻分块重叠长度（负数按 0 处理，且不超过 {@code size / 2}）
     * @return 非空分块列表，顺序与原文一致
     */
    public static List<String> split(String text, int size, int overlap) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return chunks;
        }
        int effectiveSize = size > 0 ? size : DEFAULT_SIZE;
        int effectiveOverlap = Math.max(0, overlap);
        effectiveOverlap = Math.min(effectiveOverlap, effectiveSize / 2);

        int length = text.length();
        int start = 0;
        while (start < length) {
            int windowEnd = Math.min(start + effectiveSize, length);
            int end = windowEnd;
            if (windowEnd < length) {
                int breakAt = lastBreakIndex(text, start + effectiveSize / 2, windowEnd);
                if (breakAt > start) {
                    end = breakAt + 1;
                }
            }
            String chunk = text.substring(start, end).trim();
            if (!chunk.isEmpty()) {
                chunks.add(chunk);
            }
            if (end >= length) {
                break;
            }
            // 保证前进：下一窗口起始不晚于 end，且至少推进 1 个字符
            start = Math.max(start + 1, end - effectiveOverlap);
        }
        return chunks;
    }

    /** 在 [from, to) 内从后向前找最后一个断句字符位置，未命中返回 -1。 */
    private static int lastBreakIndex(String text, int from, int to) {
        int begin = Math.max(0, from);
        for (int index = Math.min(to, text.length()) - 1; index >= begin; index--) {
            if (BREAK_CHARS.indexOf(text.charAt(index)) >= 0) {
                return index;
            }
        }
        return -1;
    }
}
