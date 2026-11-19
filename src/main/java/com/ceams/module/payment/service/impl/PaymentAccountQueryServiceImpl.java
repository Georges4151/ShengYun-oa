package com.ceams.module.payment.service.impl;

import com.ceams.module.payment.entity.PaymentAccount;
import com.ceams.module.payment.mapper.PaymentAccountMapper;
import com.ceams.module.payment.service.PaymentAccountQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** 付款账户只读查询服务实现。 */
@Service
@RequiredArgsConstructor
public class PaymentAccountQueryServiceImpl implements PaymentAccountQueryService {

    private final PaymentAccountMapper paymentAccountMapper;

    @Override
    public boolean exists(Long id) {
        if (id == null) {
            return false;
        }
        return paymentAccountMapper.selectById(id) != null;
    }

    @Override
    public PaymentAccount getById(Long id) {
        if (id == null) {
            return null;
        }
        return paymentAccountMapper.selectById(id);
    }
}
