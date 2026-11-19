package com.ceams.module.payment.service;

import com.ceams.common.response.PageResult;
import com.ceams.module.payment.dto.PaymentTransactionCreateRequest;
import com.ceams.module.payment.dto.PaymentTransactionUpdateRequest;
import com.ceams.module.payment.enums.AccountType;
import com.ceams.module.payment.vo.PaymentTransactionVO;

/** 付款交易服务。 */
public interface PaymentTransactionService {

    PageResult<PaymentTransactionVO> page(long page, long size, Long accountId, AccountType type);

    PaymentTransactionVO detail(Long id);

    String create(PaymentTransactionCreateRequest request);

    void update(Long id, PaymentTransactionUpdateRequest request);

    void delete(Long id);
}
