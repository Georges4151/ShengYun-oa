package com.ceams.module.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ceams.module.payment.entity.Account;
import org.apache.ibatis.annotations.Mapper;

/**
 * 收支流水 Mapper。
 */
@Mapper
public interface AccountMapper extends BaseMapper<Account> {
}
