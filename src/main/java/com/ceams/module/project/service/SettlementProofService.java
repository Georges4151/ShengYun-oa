package com.ceams.module.project.service;

import com.ceams.common.response.PageResult;
import com.ceams.module.project.dto.settlementproof.SettlementProofCreateRequest;
import com.ceams.module.project.dto.settlementproof.SettlementProofUpdateRequest;
import com.ceams.module.project.vo.SettlementProofVO;

/** 结算凭证服务。 */
public interface SettlementProofService {

    PageResult<SettlementProofVO> page(long page, long size, Long contractId);

    SettlementProofVO detail(Long id);

    String create(SettlementProofCreateRequest request);

    void update(Long id, SettlementProofUpdateRequest request);

    void delete(Long id);
}
