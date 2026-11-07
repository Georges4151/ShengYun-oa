package com.ceams.module.project.service;

import com.ceams.common.response.PageResult;
import com.ceams.module.project.dto.budget.BudgetCreateRequest;
import com.ceams.module.project.dto.budget.BudgetUpdateRequest;
import com.ceams.module.project.vo.BudgetVO;

/** 预算服务。 */
public interface BudgetService {

    PageResult<BudgetVO> page(long page, long size, Long projectId);

    /** 按项目经理查询预算（AI 只读工具使用，确保 STAFF 只能看到本人负责项目的预算）。 */
    PageResult<BudgetVO> pageForManager(long page, long size, Long projectId, Long managerId);

    BudgetVO detail(Long id);

    String create(BudgetCreateRequest request);

    void update(Long id, BudgetUpdateRequest request);

    void delete(Long id);
}
