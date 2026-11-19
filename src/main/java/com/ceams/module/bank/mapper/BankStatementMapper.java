package com.ceams.module.bank.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ceams.module.bank.entity.BankStatement;
import org.apache.ibatis.annotations.Mapper;

/** 银行流水 Mapper。 */
@Mapper
public interface BankStatementMapper extends BaseMapper<BankStatement> {}
