package com.ceams.module.payment.service;

import com.ceams.module.payment.entity.PaymentAccount;

/**
 * 付款账户只读查询服务（expense 域财务核验的反向依赖）。
 */
public interface PaymentAccountQueryService {

    /** 判断付款账户是否存在。 */
    boolean exists(Long id);

    /** 按 ID 查询付款账户（不存在返回 null）。 */
    PaymentAccount getById(Long id);
}
