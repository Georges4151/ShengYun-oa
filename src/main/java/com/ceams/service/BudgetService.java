package com.ceams.service;

import com.ceams.model.entity.Budget;
import com.ceams.model.Pagination;

import java.util.List;
import java.util.Map;

public interface BudgetService {
    /**
     * 获取所有预算
     * @return 预算列表
     */
    List<Budget> getAllBudgets();

    /**
     * 根据项目ID获取预算
     * @param projectId 项目ID
     * @return 预算列表
     */
    List<Budget> getByProjectId(Integer projectId);

    /**
     * 根据合同ID获取预算
     * @param contractId 合同ID
     * @return 预算列表
     */
    List<Budget> getByContractId(Integer contractId);

    /**
     * 根据ID获取预算
     * @param id 预算ID
     * @return 预算对象
     */
    Budget getBudgetById(Integer id);

    /**
     * 添加预算
     * @param budget 预算对象
     * @return 添加结果
     */
    boolean addBudget(Budget budget);

    /**
     * 更新预算
     * @param budget 预算对象
     * @return 更新结果
     */
    boolean updateBudget(Budget budget);

    /**
     * 删除预算
     * @param id 预算ID
     * @return 删除结果
     */
    boolean deleteBudget(Integer id);

    /**
     * 获取预算执行情况
     * @param budgetId 预算ID
     * @return 执行情况
     */
    Map<String, Object> getBudgetExecution(Integer budgetId);

    /**
     * 分页获取预算
     * @param pagination 分页参数
     * @return 预算列表
     */
    List<Budget> getBudgetsWithPagination(Pagination pagination);

    /**
     * 根据条件分页获取预算
     * @param projectId 项目ID
     * @param contractId 合同ID
     * @param status 状态
     * @param pagination 分页参数
     * @return 预算列表
     */
    List<Budget> getBudgetsByConditionsWithPagination(Integer projectId, Integer contractId, String status, Pagination pagination);

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
