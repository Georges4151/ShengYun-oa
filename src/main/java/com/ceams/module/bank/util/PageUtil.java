package com.ceams.module.bank.util;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;

/**
 * 分页工具：统一解析 page/size，越界抛 VALID_PAGE_BOUNDS。
 *
 * <p>bank 域独立实现，与其他域解耦。
 */
public final class PageUtil {

    public static final long MAX_SIZE = 100L;

    private PageUtil() {}

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
