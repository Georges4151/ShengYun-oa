package com.ceams.module.project.service;

import com.ceams.common.response.PageResult;
import com.ceams.module.project.dto.budget.BudgetCreateRequest;
import com.ceams.module.project.dto.budget.BudgetUpdateRequest;
import com.ceams.module.project.vo.BudgetVO;
import com.ceams.module.project.vo.IdResponse;

/**
 * 预算服务。
 */
public interface BudgetService {

    PageResult<BudgetVO> page(long page, long size, Long projectId);

    BudgetVO detail(Long id);

    IdResponse create(BudgetCreateRequest request);

    void update(Long id, BudgetUpdateRequest request);

    void delete(Long id);
}