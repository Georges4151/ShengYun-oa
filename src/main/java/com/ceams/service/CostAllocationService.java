package com.ceams.service;

import com.ceams.model.entity.CostAllocation;
import com.ceams.model.Pagination;

import java.util.List;

public interface CostAllocationService {
    /**
     * 获取所有成本分摊记录
     * @return 成本分摊列表
     */
    List<CostAllocation> getAllAllocations();

    /**
     * 根据账目记录ID获取成本分摊记录
     * @param accountRecordId 账目记录ID
     * @return 成本分摊列表
     */
    List<CostAllocation> getByAccountRecordId(Integer accountRecordId);

    /**
     * 根据目标ID和类型获取成本分摊记录
     * @param targetId 目标ID
     * @param targetType 目标类型
     * @return 成本分摊列表
     */
    List<CostAllocation> getByTargetIdAndType(Integer targetId, String targetType);

    /**
     * 根据ID获取成本分摊记录
     * @param id 成本分摊ID
     * @return 成本分摊对象
     */
    CostAllocation getAllocationById(Integer id);

    /**
     * 添加成本分摊记录
     * @param costAllocation 成本分摊对象
     * @return 添加结果
     */
    boolean addAllocation(CostAllocation costAllocation);

    /**
     * 更新成本分摊记录
     * @param costAllocation 成本分摊对象
     * @return 更新结果
     */
    boolean updateAllocation(CostAllocation costAllocation);

    /**
     * 删除成本分摊记录
     * @param id 成本分摊ID
     * @return 删除结果
     */
    boolean deleteAllocation(Integer id);

    /**
     * 分页获取成本分摊记录
     * @param pagination 分页参数
     * @return 成本分摊列表
     */
    List<CostAllocation> getAllocationsWithPagination(Pagination pagination);

    /**
     * 根据条件分页获取成本分摊记录
     * @param accountRecordId 账目记录ID
     * @param targetType 目标类型
     * @param pagination 分页参数
     * @return 成本分摊列表
     */
    List<CostAllocation> getAllocationsByConditionsWithPagination(Integer accountRecordId, String targetType, Pagination pagination);

    /**
     * 获取成本分摊记录总数
     * @return 成本分摊记录总数
     */
    long count();

    /**
     * 根据条件获取成本分摊记录总数
     * @param accountRecordId 账目记录ID
     * @param targetType 目标类型
     * @return 成本分摊记录总数
     */
    int countByConditions(Integer accountRecordId, String targetType);
}
