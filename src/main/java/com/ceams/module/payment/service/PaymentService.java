package com.ceams.module.payment.service;

import com.ceams.module.payment.dto.PaymentExecuteRequest;

/** 付款执行服务。 */
public interface PaymentService {

    /**
     * 执行付款：校验账户、幂等、余额、更新余额、写付款交易、写收支流水。
     *
     * @return 新付款交易 ID（字符串）
     */
    String execute(PaymentExecuteRequest request);
}
