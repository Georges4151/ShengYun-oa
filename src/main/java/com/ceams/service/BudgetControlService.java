package com.ceams.service;

import com.ceams.model.entity.Budget;
import com.ceams.model.entity.Account;

import java.util.List;
import java.util.Map;

public interface BudgetControlService {
    /**
     * 检查预算是否足够
     * @param projectId 项目ID
     * @param contractId 合同ID
     * @param amount 金额
     * @return 检查结果
     */
    Map<String, Object> checkBudgetAvailability(Integer projectId, Integer contractId, java.math.BigDecimal amount);

    /**
     * 应用预算控制
     * @param account 账目记录
     * @return 控制结果
     */
    boolean applyBudgetControl(Account account);

    /**
     * 获取预算预警
     * @return 预算预警列表
     */
    List<Map<String, Object>> getBudgetAlerts();

    /**
     * 生成预算使用报告
     * @param budgetId 预算ID
     * @return 预算使用报告
     */
    Map<String, Object> generateBudgetUsageReport(Integer budgetId);
}
