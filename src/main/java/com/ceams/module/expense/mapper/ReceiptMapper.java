package com.ceams.module.expense.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ceams.module.expense.entity.Receipt;
import org.apache.ibatis.annotations.Mapper;

/**
 * 票据 Mapper。
 */
@Mapper
public interface ReceiptMapper extends BaseMapper<Receipt> {
}
