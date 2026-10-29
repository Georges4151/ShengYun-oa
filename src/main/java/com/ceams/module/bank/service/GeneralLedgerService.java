package com.ceams.module.bank.service;

import com.ceams.common.response.PageResult;
import com.ceams.module.bank.dto.GeneralLedgerCreateRequest;
import com.ceams.module.bank.dto.GeneralLedgerUpdateRequest;
import com.ceams.module.bank.enums.TransactionType;
import com.ceams.module.bank.vo.GeneralLedgerVO;

/** 总账服务。 */
public interface GeneralLedgerService {

    PageResult<GeneralLedgerVO> page(
            long page,
            long size,
            String accountName,
            TransactionType transactionType,
            String status);

    GeneralLedgerVO detail(Long id);

    String create(GeneralLedgerCreateRequest request);

    void update(Long id, GeneralLedgerUpdateRequest request);

    void delete(Long id);
}
