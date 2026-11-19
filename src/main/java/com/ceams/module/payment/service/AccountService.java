package com.ceams.module.payment.service;

import com.ceams.common.response.PageResult;
import com.ceams.module.payment.dto.AccountCreateRequest;
import com.ceams.module.payment.dto.AccountUpdateRequest;
import com.ceams.module.payment.enums.AccountStatus;
import com.ceams.module.payment.enums.AccountType;
import com.ceams.module.payment.vo.AccountVO;

/** 收支流水服务。 */
public interface AccountService {

    PageResult<AccountVO> page(
            long page, long size, Long projectId, AccountType type, AccountStatus status);

    AccountVO detail(Long id);

    String create(AccountCreateRequest request);

    void update(Long id, AccountUpdateRequest request);

    void delete(Long id);

    void confirm(Long id);

    void cancel(Long id);
}
