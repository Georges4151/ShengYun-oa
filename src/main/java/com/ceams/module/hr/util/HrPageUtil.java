package com.ceams.module.hr.util;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;

/**
 * HR 域分页工具：统一解析 page/size，越界抛 VALID_PAGE_BOUNDS。
 *
 * <p>按域独立实现，避免跨模块耦合（与 expense / masterdata 的 PageUtil 口径一致）。
 */
public final class HrPageUtil {

    /** 单页最大条数。 */
    public static final long MAX_SIZE = 100L;

    private HrPageUtil() {}

    public static <T> Page<T> resolve(long page, long size) {
        if (page < 1) {
            throw BusinessException.of(ErrorCode.VALID_PAGE_BOUNDS, "页码必须不小于 1");
        }
        if (size < 1) {
            throw BusinessException.of(ErrorCode.VALID_PAGE_BOUNDS, "每页条数必须不小于 1");
        }
        if (size > MAX_SIZE) {
            throw BusinessException.of(ErrorCode.VALID_PAGE_BOUNDS, "每页条数不能超过 " + MAX_SIZE);
        }
        return new Page<>(page, size);
    }
}
