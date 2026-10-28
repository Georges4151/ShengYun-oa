package com.ceams.module.payment.service;

import com.ceams.common.response.PageResult;
import com.ceams.module.payment.dto.CostAllocationCreateRequest;
import com.ceams.module.payment.dto.CostAllocationUpdateRequest;
import com.ceams.module.payment.vo.CostAllocationVO;

/**
 * 成本分摊服务。
 */
public interface CostAllocationService {

    PageResult<CostAllocationVO> page(long page, long size, Long accountRecordId);

    CostAllocationVO detail(Long id);

    String create(CostAllocationCreateRequest request);

    void update(Long id, CostAllocationUpdateRequest request);

    void delete(Long id);
}
