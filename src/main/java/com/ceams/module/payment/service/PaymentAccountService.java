package com.ceams.module.payment.service;

import com.ceams.common.response.PageResult;
import com.ceams.module.payment.dto.PaymentAccountCreateRequest;
import com.ceams.module.payment.dto.PaymentAccountUpdateRequest;
import com.ceams.module.payment.vo.PaymentAccountVO;

/**
 * 付款账户服务。
 */
public interface PaymentAccountService {

    PageResult<PaymentAccountVO> page(long page, long size, String name, String type);

    PaymentAccountVO detail(Long id);

    String create(PaymentAccountCreateRequest request);

    void update(Long id, PaymentAccountUpdateRequest request);

    void delete(Long id);
}
