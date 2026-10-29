package com.ceams.module.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ceams.module.payment.entity.PaymentTransaction;
import org.apache.ibatis.annotations.Mapper;

/** 付款交易 Mapper。 */
@Mapper
public interface PaymentTransactionMapper extends BaseMapper<PaymentTransaction> {}
