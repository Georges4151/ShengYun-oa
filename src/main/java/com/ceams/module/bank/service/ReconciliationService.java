package com.ceams.module.bank.service;

import com.ceams.common.response.PageResult;
import com.ceams.module.bank.dto.ReconciliationMatchRequest;
import com.ceams.module.bank.dto.ReconciliationResolveRequest;
import com.ceams.module.bank.enums.ReconciliationStatus;
import com.ceams.module.bank.enums.ResolutionStatus;
import com.ceams.module.bank.vo.ReconciliationMatchResultVO;
import com.ceams.module.bank.vo.ReconciliationResultVO;

/** 对账结果服务。 */
public interface ReconciliationService {

    PageResult<ReconciliationResultVO> page(
            long page,
            long size,
            Long accountId,
            ReconciliationStatus status,
            ResolutionStatus resolutionStatus);

    ReconciliationResultVO detail(Long id);

    ReconciliationMatchResultVO match(ReconciliationMatchRequest request);

    void resolve(Long id, ReconciliationResolveRequest request);
}
