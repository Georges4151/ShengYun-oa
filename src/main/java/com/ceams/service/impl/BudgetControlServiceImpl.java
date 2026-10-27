package com.ceams.service.impl;

import com.ceams.service.BudgetControlService;
import com.ceams.service.BudgetService;
import com.ceams.model.entity.Budget;
import com.ceams.model.entity.Account;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class BudgetControlServiceImpl implements BudgetControlService {

    @Autowired
    private BudgetService budgetService;

    @Override
    public Map<String, Object> checkBudgetAvailability(Integer projectId, Integer contractId, BigDecimal amount) {
        Map<String, Object> result = new HashMap<>();
        boolean isAvailable = true;
        List<Budget> relevantBudgets = new ArrayList<>();
        BigDecimal totalAvailableBudget = BigDecimal.ZERO;

        // 获取相关预算
        if (projectId != null) {
            relevantBudgets.addAll(budgetService.getByProjectId(projectId));
        }
        if (contractId != null) {
            relevantBudgets.addAll(budgetService.getByContractId(contractId));
        }

        // 计算可用预算
        for (Budget budget : relevantBudgets) {
            if ("active".equals(budget.getStatus())) {
                Map<String, Object> execution = budgetService.getBudgetExecution(budget.getId());
                BigDecimal usedAmount = (BigDecimal) execution.getOrDefault("used_amount", BigDecimal.ZERO);
                BigDecimal available = budget.getAmount().subtract(usedAmount);
                totalAvailableBudget = totalAvailableBudget.add(available);
            }
        }

        // 检查预算是否足够
        if (totalAvailableBudget.compareTo(amount) < 0) {
            isAvailable = false;
        }

        result.put("isAvailable", isAvailable);
        result.put("requiredAmount", amount);
        result.put("availableBudget", totalAvailableBudget);
        result.put("relevantBudgets", relevantBudgets);
        result.put("status", isAvailable ? "success" : "error");

        return result;
    }

    @Override
    public boolean applyBudgetControl(Account account) {
        // 检查是否需要预算控制
        if (account.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return true; // 不需要控制
        }

        // 检查预算是否足够
        Map<String, Object> budgetCheck = checkBudgetAvailability(
                account.getProjectId(), account.getContractId(), account.getAmount());

        if (!(boolean) budgetCheck.get("isAvailable")) {
            return false; // 预算不足
        }

        // 这里可以添加更多的预算控制逻辑
        // 例如：更新预算使用情况、发送预算预警等

        return true;
    }

    @Override
    public List<Map<String, Object>> getBudgetAlerts() {
        List<Map<String, Object>> alerts = new ArrayList<>();

        // 获取所有预算
        List<Budget> budgets = budgetService.getAllBudgets();

        for (Budget budget : budgets) {
            if ("active".equals(budget.getStatus())) {
                Map<String, Object> execution = budgetService.getBudgetExecution(budget.getId());
                BigDecimal usedAmount = (BigDecimal) execution.getOrDefault("used_amount", BigDecimal.ZERO);
                BigDecimal budgetAmount = budget.getAmount();
                BigDecimal usageRate = budgetAmount.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO
                        : usedAmount.divide(budgetAmount, 4, RoundingMode.HALF_UP);

                // 生成预警
                if (usageRate.compareTo(new BigDecimal("0.8")) >= 0) {
                    Map<String, Object> alert = new HashMap<>();
                    alert.put("budgetId", budget.getId());
                    alert.put("budgetName", budget.getBudgetName());
                    alert.put("budgetAmount", budgetAmount);
                    alert.put("usedAmount", usedAmount);
                    alert.put("usageRate", usageRate);
                    alert.put("alertLevel", usageRate.compareTo(new BigDecimal("0.9")) >= 0 ? "high" : "medium");
                    alert.put("message", "预算使用接近上限");
                    alerts.add(alert);
                }
            }
        }

        return alerts;
    }

    @Override
    public Map<String, Object> generateBudgetUsageReport(Integer budgetId) {
        Map<String, Object> report = new HashMap<>();

        // 获取预算信息
        Budget budget = budgetService.getBudgetById(budgetId);
        if (budget == null) {
            report.put("status", "error");
            report.put("message", "预算不存在");
            return report;
        }

        // 获取预算执行情况
        Map<String, Object> execution = budgetService.getBudgetExecution(budgetId);

        report.put("budget", budget);
        report.put("execution", execution);
        report.put("status", "success");

        return report;
    }
}
