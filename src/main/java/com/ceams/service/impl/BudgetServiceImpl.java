package com.ceams.service.impl;

import com.ceams.dao.BudgetMapper;
import com.ceams.model.entity.Budget;
import com.ceams.model.Pagination;
import com.ceams.service.BudgetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class BudgetServiceImpl implements BudgetService {

    @Autowired
    private BudgetMapper budgetMapper;

    @Override
    public List<Budget> getAllBudgets() {
        return budgetMapper.selectAll();
    }

    @Override
    public List<Budget> getByProjectId(Integer projectId) {
        return budgetMapper.selectByProjectId(projectId);
    }

    @Override
    public List<Budget> getByContractId(Integer contractId) {
        return budgetMapper.selectByContractId(contractId);
    }

    @Override
    public Budget getBudgetById(Integer id) {
        return budgetMapper.selectById(id);
    }

    @Override
    public boolean addBudget(Budget budget) {
        // 设置默认值
        if (budget.getStatus() == null) {
            budget.setStatus("active");
        }
        return budgetMapper.insert(budget) > 0;
    }

    @Override
    public boolean updateBudget(Budget budget) {
        return budgetMapper.update(budget) > 0;
    }

    @Override
    public boolean deleteBudget(Integer id) {
        return budgetMapper.deleteById(id) > 0;
    }

    @Override
    public Map<String, Object> getBudgetExecution(Integer budgetId) {
        return budgetMapper.getBudgetExecution(budgetId);
    }

    @Override
    public List<Budget> getBudgetsWithPagination(Pagination pagination) {
        return budgetMapper.selectAllWithPagination(pagination);
    }

    @Override
    public List<Budget> getBudgetsByConditionsWithPagination(Integer projectId, Integer contractId, String status, Pagination pagination) {
        return budgetMapper.selectByConditionsWithPagination(projectId, contractId, status, pagination);
    }

    @Override
    public long count() {
        return budgetMapper.count();
    }

    @Override
    public int countByConditions(Integer projectId, Integer contractId, String status) {
        return budgetMapper.countByConditions(projectId, contractId, status);
    }
}
