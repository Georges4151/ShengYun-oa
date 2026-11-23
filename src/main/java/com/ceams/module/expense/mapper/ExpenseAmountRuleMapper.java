package com.ceams.module.expense.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ceams.module.expense.entity.ExpenseAmountRule;
import org.apache.ibatis.annotations.Mapper;

/** 报销额度分级规则 Mapper。 */
@Mapper
public interface ExpenseAmountRuleMapper extends BaseMapper<ExpenseAmountRule> {}
