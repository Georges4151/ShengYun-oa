package com.ceams.dao;

import com.ceams.model.entity.CostAllocation;
import com.ceams.model.Pagination;

import java.util.List;

public interface CostAllocationMapper {
    /**
     * 插入成本分摊记录
     * @param costAllocation 成本分摊对象
     * @return 影响行数
     */
    int insert(CostAllocation costAllocation);

    /**
     * 更新成本分摊记录
     * @param costAllocation 成本分摊对象
     * @return 影响行数
     */
    int update(CostAllocation costAllocation);

    /**
     * 根据ID删除成本分摊记录
     * @param id 成本分摊ID
     * @return 影响行数
     */
    int deleteById(Integer id);

    /**
     * 根据ID查询成本分摊记录
     * @param id 成本分摊ID
     * @return 成本分摊对象
     */
    CostAllocation selectById(Integer id);

    /**
     * 查询所有成本分摊记录
     * @return 成本分摊列表
     */
    List<CostAllocation> selectAll();

    /**
     * 根据账目记录ID查询成本分摊记录
     * @param accountRecordId 账目记录ID
     * @return 成本分摊列表
     */
    List<CostAllocation> selectByAccountRecordId(Integer accountRecordId);

    /**
     * 根据目标ID和类型查询成本分摊记录
     * @param targetId 目标ID
     * @param targetType 目标类型
     * @return 成本分摊列表
     */
    List<CostAllocation> selectByTargetIdAndType(Integer targetId, String targetType);

    /**
     * 分页查询成本分摊记录
     * @param pagination 分页参数
     * @return 成本分摊列表
     */
    List<CostAllocation> selectAllWithPagination(Pagination pagination);

    /**
     * 根据条件分页查询成本分摊记录
     * @param accountRecordId 账目记录ID
     * @param targetType 目标类型
     * @param pagination 分页参数
     * @return 成本分摊列表
     */
    List<CostAllocation> selectByConditionsWithPagination(Integer accountRecordId, String targetType, Pagination pagination);

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
