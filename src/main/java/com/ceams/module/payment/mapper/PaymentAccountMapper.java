package com.ceams.module.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ceams.module.payment.entity.PaymentAccount;
import org.apache.ibatis.annotations.Mapper;

/** 付款账户 Mapper。 */
@Mapper
public interface PaymentAccountMapper extends BaseMapper<PaymentAccount> {}
