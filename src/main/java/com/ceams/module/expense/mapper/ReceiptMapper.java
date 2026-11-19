package com.ceams.module.expense.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ceams.module.expense.entity.Receipt;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 票据 Mapper。 */
@Mapper
public interface ReceiptMapper extends BaseMapper<Receipt> {

    /** 加行锁读取票据，串行化并发提交，避免重复发起 BPM 实例。 */
    @Select("SELECT * FROM biz_receipt WHERE id = #{id} AND deleted = 0 FOR UPDATE")
    Receipt selectByIdForUpdate(@Param("id") Long id);
}
