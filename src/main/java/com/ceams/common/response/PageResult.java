package com.ceams.common.response;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Getter;
import lombok.Setter;

import java.util.Collections;
import java.util.List;

/**
 * 统一分页结果结构。
 *
 * <pre>{@code
 * { "records": [], "total": 0, "page": 1, "size": 20 }
 * }</pre>
 *
 * @param <T> 记录类型
 */
@Getter
@Setter
public class PageResult<T> {

    /** 当前页记录（空列表返回 []，不返回 null）。 */
    private List<T> records;

    /** 总条数。 */
    private long total;

    /** 当前页码（从 1 开始）。 */
    private long page;

    /** 每页条数。 */
    private long size;

    public PageResult() {
    }

    public PageResult(List<T> records, long total, long page, long size) {
        this.records = records == null ? Collections.emptyList() : records;
        this.total = total;
        this.page = page;
        this.size = size;
    }

    /** 从 MyBatis-Plus 分页结果构建。 */
    public static <T> PageResult<T> of(IPage<T> page) {
        return new PageResult<>(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }
}