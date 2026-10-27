package com.ceams.dao;

import com.ceams.model.entity.Budget;
import com.ceams.model.Pagination;

import java.util.List;
import java.util.Map;

public interface BudgetMapper {
    /**
     * 插入预算记录
     * @param budget 预算对象
     * @return 影响行数
     */
    int insert(Budget budget);

    /**
     * 更新预算记录
     * @param budget 预算对象
     * @return 影响行数
     */
    int update(Budget budget);

    /**
     * 根据ID删除预算记录
     * @param id 预算ID
     * @return 影响行数
     */
    int deleteById(Integer id);

    /**
     * 根据ID查询预算记录
     * @param id 预算ID
     * @return 预算对象
     */
    Budget selectById(Integer id);

    /**
     * 查询所有预算记录
     * @return 预算列表
     */
    List<Budget> selectAll();

    /**
     * 根据项目ID查询预算记录
     * @param projectId 项目ID
     * @return 预算列表
     */
    List<Budget> selectByProjectId(Integer projectId);

    /**
     * 根据合同ID查询预算记录
     * @param contractId 合同ID
     * @return 预算列表
     */
    List<Budget> selectByContractId(Integer contractId);

    /**
     * 分页查询预算记录
     * @param pagination 分页参数
     * @return 预算列表
     */
    List<Budget> selectAllWithPagination(Pagination pagination);

    /**
     * 根据条件分页查询预算记录
     * @param projectId 项目ID
     * @param contractId 合同ID
     * @param status 状态
     * @param pagination 分页参数
     * @return 预算列表
     */
    List<Budget> selectByConditionsWithPagination(Integer projectId, Integer contractId, String status, Pagination pagination);

    /**
     * 获取预算执行情况
     * @param budgetId 预算ID
     * @return 执行情况Map
     */
    Map<String, Object> getBudgetExecution(Integer budgetId);

    /**
     * 获取预算总数
     * @return 预算总数
     */
    long count();

    /**
     * 根据条件获取预算总数
     * @param projectId 项目ID
     * @param contractId 合同ID
     * @param status 状态
     * @return 预算总数
     */
    int countByConditions(Integer projectId, Integer contractId, String status);
}
