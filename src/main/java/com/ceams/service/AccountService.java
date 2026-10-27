package com.ceams.service;

import com.ceams.dao.AccountMapper;
import com.ceams.exception.BusinessException;
import com.ceams.model.Pagination;
import com.ceams.model.entity.Account;
import com.ceams.model.entity.Project;
import com.ceams.model.entity.SubcontractorContract;
import com.ceams.model.event.AccountDataChangedEvent;
import com.ceams.service.SubcontractorService;
import com.ceams.service.BudgetControlService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
public class AccountService {

    @Autowired
    private AccountMapper accountMapper;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private SubcontractorService subcontractorService;

    @Autowired
    private BudgetControlService budgetControlService;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    public List<Account> getByProjectId(Integer projectId) {
        return accountMapper.selectByProjectId(projectId);
    }

    /**
     * 根据项目ID更新项目实际金额
     * 
     * @param projectId 项目ID
     */
    private void updateProjectActualAmount(Integer projectId) {
        if (projectId == null) {
            return;
        }

        try {
            // 获取项目账目汇总信息
            Map<String, Object> summary = accountMapper.getAccountSummary(projectId, null, null);
            if (summary != null) {
                // 获取总收入和总支出，处理不同类型的数值
                Object totalIncomeObj = summary.get("total_income");
                Object totalExpenseObj = summary.get("total_expense");

                BigDecimal totalIncome = BigDecimal.ZERO;
                BigDecimal totalExpense = BigDecimal.ZERO;

                // 处理总收入
                if (totalIncomeObj != null) {
                    if (totalIncomeObj instanceof BigDecimal) {
                        totalIncome = (BigDecimal) totalIncomeObj;
                    } else if (totalIncomeObj instanceof Double) {
                        totalIncome = BigDecimal.valueOf((Double) totalIncomeObj);
                    } else if (totalIncomeObj instanceof Integer) {
                        totalIncome = BigDecimal.valueOf((Integer) totalIncomeObj);
                    } else {
                        // 尝试将其他类型转换为BigDecimal
                        totalIncome = new BigDecimal(totalIncomeObj.toString());
                    }
                }

                // 处理总支出
                if (totalExpenseObj != null) {
                    if (totalExpenseObj instanceof BigDecimal) {
                        totalExpense = (BigDecimal) totalExpenseObj;
                    } else if (totalExpenseObj instanceof Double) {
                        totalExpense = BigDecimal.valueOf((Double) totalExpenseObj);
                    } else if (totalExpenseObj instanceof Integer) {
                        totalExpense = BigDecimal.valueOf((Integer) totalExpenseObj);
                    } else {
                        // 尝试将其他类型转换为BigDecimal
                        totalExpense = new BigDecimal(totalExpenseObj.toString());
                    }
                }

                // 根据用户需求计算实际金额：入账时实际金额减少，出账时实际金额增加
                // 实际金额 = 总支出 - 总收入
                BigDecimal actualAmount = totalExpense.subtract(totalIncome);

                System.out.println("项目ID " + projectId + " 的实际金额: " + actualAmount);

                // 获取项目详情
                Project project = projectService.getProjectDetail(projectId);
                if (project != null) {
                    // 更新项目实际金额
                    project.setActualAmount(actualAmount);
                    boolean result = projectService.updateProject(project);
                    System.out.println("更新项目实际金额结果: " + result);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("更新项目实际金额失败: " + e.getMessage());
        }
    }

    public List<Account> getByDateRange(Date startDate, Date endDate) {
        return accountMapper.selectByDateRange(startDate, endDate);
    }

    public List<Account> getByType(String type) {
        return accountMapper.selectByType(type);
    }

    public Map<String, Object> getAccountSummary(Integer projectId, Date startDate, Date endDate) {
        return accountMapper.getAccountSummary(projectId, startDate, endDate);
    }

    public List<Map<String, Object>> getMonthlyStats(Integer year) {
        return accountMapper.getMonthlyStats(year);
    }

    public boolean addAccount(Account account) {
        return addAccount(account, true);
    }

    public boolean addAccount(Account account, boolean applyBudgetControl) {
        // 只有 applyBudgetControl 为 true 时才检查支出账目（手动添加需校验）
        // applyBudgetControl 为 false 时跳过此检查（系统自动创建如核销时不校验）
        if (applyBudgetControl && "expense".equals(account.getType())) {
            throw new BusinessException("EXPENSE_NOT_ALLOWED",
                    "支出账目请通过票据上传流程添加。请前往【票据管理】上传票据信息。");
        }

        if (account.getStatus() == null) {
            account.setStatus("draft");
        }

        if (applyBudgetControl && "expense".equals(account.getType())) {
            boolean budgetCheck = budgetControlService.applyBudgetControl(account);
            if (!budgetCheck) {
                return false;
            }
        }

        boolean result = accountMapper.insert(account) > 0;
        if (result) {
            updateProjectActualAmount(account.getProjectId());
            eventPublisher.publishEvent(new AccountDataChangedEvent(this, "添加了新账目记录"));
        }
        return result;
    }

    public boolean updateAccount(Account account) {
        boolean result = accountMapper.update(account) > 0;
        if (result) {
            // 更新项目实际金额
            updateProjectActualAmount(account.getProjectId());
            // 发布账目数据变化事件
            eventPublisher.publishEvent(new AccountDataChangedEvent(this, "更新了账目记录"));
        }
        return result;
    }

    public boolean deleteAccount(Integer accountId) {
        // 获取要删除的账目记录
        Account account = accountMapper.selectById(accountId);
        Integer projectId = null;
        if (account != null) {
            projectId = account.getProjectId();
        }

        boolean result = accountMapper.deleteById(accountId) > 0;
        if (result) {
            if (projectId != null) {
                // 更新项目实际金额
                updateProjectActualAmount(projectId);
            }
            // 发布账目数据变化事件
            eventPublisher.publishEvent(new AccountDataChangedEvent(this, "删除了账目记录"));
        }
        return result;
    }

    public Account getAccountDetail(Integer accountId) {
        return accountMapper.selectById(accountId);
    }

    public List<Account> searchAccounts(Map<String, Object> params) {
        // 从参数中提取筛选条件
        Integer projectId = params != null ? (Integer) params.get("projectId") : null;
        Date startDate = params != null ? (Date) params.get("startDate") : null;
        Date endDate = params != null ? (Date) params.get("endDate") : null;

        // 创建一个临时的分页对象，使用一个很大的pageSize来获取所有符合条件的记录
        Pagination pagination = new Pagination();
        pagination.setPageSize(10000); // 足够大的页码，确保获取所有记录
        pagination.setPageNum(1);

        // 使用现有的条件查询方法
        return accountMapper.selectByConditionsWithPagination(projectId, null, startDate, endDate,
                pagination.getPageSize(), pagination.getOffset());
    }

    public List<Account> getAllAccounts() {
        return accountMapper.selectAll();
    }

    /**
     * 分页获取所有账目记录
     * 
     * @param pagination 分页参数
     * @return 账目记录列表
     */
    public List<Account> getAccountsWithPagination(Pagination pagination) {
        return accountMapper.selectAllWithPagination(pagination.getPageSize(), pagination.getOffset());
    }

    /**
     * 分页获取账目记录（支持条件）
     * 
     * @param projectId  项目ID
     * @param type       账目类型
     * @param startDate  开始日期
     * @param endDate    结束日期
     * @param pagination 分页参数
     * @return 账目记录列表
     */
    public List<Account> getAccountsByConditionsWithPagination(
            Integer projectId, String type, Date startDate, Date endDate, Pagination pagination) {
        return accountMapper.selectByConditionsWithPagination(
                projectId, type, startDate, endDate, pagination.getPageSize(), pagination.getOffset());
    }

    /**
     * 获取账目记录总数（支持条件）
     * 
     * @param projectId 项目ID
     * @param type      账目类型
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 账目记录总数
     */
    public int getAccountCountByConditions(
            Integer projectId, String type, Date startDate, Date endDate) {
        return accountMapper.countByConditions(projectId, type, startDate, endDate);
    }

    /**
     * 获取所有账目记录总数
     * 
     * @return 账目记录总数
     */
    public long count() {
        return accountMapper.count();
    }

    /**
     * 根据合同ID获取账目记录
     * 
     * @param contractId 合同ID
     * @return 账目记录列表
     */
    public List<Account> getByContractId(Integer contractId) {
        return accountMapper.selectByContractId(contractId);
    }

    /**
     * 从合同创建账目记录
     * 
     * @param account 账目对象
     * @return 创建结果
     */
    public boolean addAccountFromContract(Account account) {
        // 设置默认值
        if (account.getStatus() == null) {
            account.setStatus("completed");
        }
        if (account.getType() == null) {
            account.setType("expense");
        }

        // 应用预算控制
        if ("expense".equals(account.getType())) {
            boolean budgetCheck = budgetControlService.applyBudgetControl(account);
            if (!budgetCheck) {
                return false; // 预算不足，拒绝添加
            }
        }

        boolean result = accountMapper.insert(account) > 0;
        if (result) {
            // 更新项目实际金额
            updateProjectActualAmount(account.getProjectId());
            // 更新合同已支付金额
            updateContractPaidAmount(account.getContractId());
            // 发布账目数据变化事件
            eventPublisher.publishEvent(new AccountDataChangedEvent(this, "从合同创建了新账目记录"));
        }
        return result;
    }

    /**
     * 更新合同已支付金额
     * 
     * @param contractId 合同ID
     * @return 更新结果
     */
    public boolean updateContractPaidAmount(Integer contractId) {
        if (contractId == null) {
            return false;
        }

        try {
            // 获取合同相关的所有支出账目
            List<Account> accounts = accountMapper.selectByContractId(contractId);
            if (accounts == null || accounts.isEmpty()) {
                return false;
            }

            // 计算已支付总金额
            java.math.BigDecimal totalPaid = java.math.BigDecimal.ZERO;
            for (Account account : accounts) {
                if ("expense".equals(account.getType()) && "completed".equals(account.getStatus())) {
                    totalPaid = totalPaid.add(account.getAmount());
                }
            }

            // 更新合同已支付金额
            SubcontractorContract contract = subcontractorService.getContractDetail(contractId);
            if (contract != null) {
                contract.setPaidAmount(totalPaid);
                return subcontractorService.updateContract(contract);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("更新合同已支付金额失败: " + e.getMessage());
        }
        return false;
    }

    public List<Account> searchAccounts(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllAccounts();
        }
        return accountMapper.searchByKeyword(keyword.trim());
    }
}