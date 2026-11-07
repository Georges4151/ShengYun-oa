package com.ceams.module.ai.tool;

import com.ceams.common.response.PageResult;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.IntFunction;

/**
 * 分页收集器：业务模块统一限制每页最多 100 行，工具按最多 200 行分两页读取。
 *
 * <p>调用方传入按页码加载的函数，本类负责截断并汇总真实 total。
 */
public final class PageCollector {

    /** 业务模块 PageUtil 的每页上限。 */
    public static final int MAX_PAGE_SIZE = 100;

    /** 工具层最多读取两页。 */
    private static final int MAX_PAGES = 2;

    private PageCollector() {}

    /** 最多读取 {@code limit} 行，返回命中总数与是否截断。 */
    public static <T> Result<T> collect(IntFunction<PageResult<T>> loader, int limit) {
        List<T> items = new ArrayList<>();
        long total = 0L;
        int page = 1;
        while (items.size() < limit && page <= MAX_PAGES) {
            PageResult<T> result = loader.apply(page);
            if (result == null) {
                break;
            }
            total = result.getTotal();
            List<T> records =
                    result.getRecords() == null ? Collections.emptyList() : result.getRecords();
            if (records.isEmpty()) {
                break;
            }
            int need = limit - items.size();
            items.addAll(records.size() <= need ? records : records.subList(0, need));
            if (items.size() >= total || records.size() < result.getSize()) {
                break;
            }
            page++;
        }
        return new Result<>(items, total, items.size() < total);
    }

    /** 分页收集结果。 */
    public record Result<T>(List<T> items, long total, boolean truncated) {

        public Result {
            items = items == null ? Collections.emptyList() : List.copyOf(items);
        }
    }
}
