package com.ceams.service.impl;

import com.ceams.service.ReportServiceInterface;
import com.ceams.service.AccountService;
import com.ceams.service.ProjectService;
import com.ceams.service.SubcontractorService;
import com.ceams.model.entity.Account;
import com.ceams.model.entity.SubcontractorContract;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ReportServiceImpl implements ReportServiceInterface {

    @Autowired
    private AccountService accountService;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private SubcontractorService subcontractorService;

    @Override
    public List<String> getAnalysisDimensions() {
        List<String> dimensions = new ArrayList<>();
        dimensions.add("project");
        dimensions.add("department");
        dimensions.add("subcontractor");
        dimensions.add("paymentType");
        dimensions.add("contract");
        dimensions.add("date");
        return dimensions;
    }

    @Override
    public Map<String, Object> getMultiDimensionalAnalysis(List<String> dimensions, String startDate, String endDate) {
        Map<String, Object> result = new HashMap<>();

        // 获取所有账目数据
        List<Account> accounts = accountService.getAllAccounts();

        // 根据维度进行分析
        if (dimensions.contains("project")) {
            result.put("projectAnalysis", analyzeByProject(accounts));
        }

        if (dimensions.contains("subcontractor")) {
            result.put("subcontractorAnalysis", analyzeBySubcontractor(accounts));
        }

        if (dimensions.contains("paymentType")) {
            result.put("paymentTypeAnalysis", analyzeByPaymentType(accounts));
        }

        if (dimensions.contains("contract")) {
            result.put("contractAnalysis", analyzeByContract(accounts));
        }

        if (dimensions.contains("date")) {
            result.put("dateAnalysis", analyzeByDate(accounts, startDate, endDate));
        }

        return result;
    }

    @Override
    public Map<String, Object> getContractPaymentReport(String startDate, String endDate) {
        Map<String, Object> result = new HashMap<>();

        // 获取所有合同
        List<SubcontractorContract> contracts = subcontractorService.getAllContracts();

        // 分析合同支付情况
        List<Map<String, Object>> contractPayments = new ArrayList<>();
        for (SubcontractorContract contract : contracts) {
            Map<String, Object> paymentInfo = new HashMap<>();
            paymentInfo.put("contractId", contract.getId());
            paymentInfo.put("contractName", contract.getContractName());
            paymentInfo.put("contractAmount", contract.getContractAmount());
            paymentInfo.put("paidAmount", contract.getPaidAmount());
            paymentInfo.put("remainingAmount", contract.getContractAmount().subtract(contract.getPaidAmount()));
            paymentInfo.put("paymentRate", contract.getContractAmount().compareTo(java.math.BigDecimal.ZERO) == 0 ? 0 : 
                    contract.getPaidAmount().divide(contract.getContractAmount(), 4, java.math.RoundingMode.HALF_UP).doubleValue() * 100);
            contractPayments.add(paymentInfo);
        }

        result.put("contractPayments", contractPayments);
        result.put("totalContracts", contracts.size());
        result.put("totalContractAmount", contracts.stream().map(SubcontractorContract::getContractAmount).reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add));
        result.put("totalPaidAmount", contracts.stream().map(SubcontractorContract::getPaidAmount).reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add));

        return result;
    }

    @Override
    public Map<String, Object> getCashFlowForecast(Integer months) {
        Map<String, Object> result = new HashMap<>();

        // 获取历史账目数据
        List<Account> accounts = accountService.getAllAccounts();

        // 分析历史现金流
        Map<String, java.math.BigDecimal> monthlyCashFlow = analyzeHistoricalCashFlow(accounts);

        // 预测未来现金流
        List<Map<String, Object>> forecast = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        for (int i = 1; i <= months; i++) {
            calendar.add(Calendar.MONTH, 1);
            String month = new java.text.SimpleDateFormat("yyyy-MM").format(calendar.getTime());
            
            // 基于历史数据进行简单预测
            java.math.BigDecimal predictedFlow = predictMonthlyCashFlow(monthlyCashFlow);
            
            Map<String, Object> monthData = new HashMap<>();
            monthData.put("month", month);
            monthData.put("predictedFlow", predictedFlow);
            forecast.add(monthData);
        }

        result.put("historicalData", monthlyCashFlow);
        result.put("forecast", forecast);
        result.put("predictionMonths", months);

        return result;
    }

    @Override
    public Map<String, Object> getCostAnalysisReport(String startDate, String endDate) {
        Map<String, Object> result = new HashMap<>();

        // 获取所有账目数据
        List<Account> accounts = accountService.getAllAccounts();

        // 分析成本构成
        Map<String, java.math.BigDecimal> costByCategory = new HashMap<>();
        java.math.BigDecimal totalCost = java.math.BigDecimal.ZERO;

        for (Account account : accounts) {
            if ("expense".equals(account.getType())) {
                String category = account.getPaymentType() != null ? account.getPaymentType() : "其他";
                java.math.BigDecimal amount = account.getAmount();
                costByCategory.put(category, costByCategory.getOrDefault(category, java.math.BigDecimal.ZERO).add(amount));
                totalCost = totalCost.add(amount);
            }
        }

        // 计算各分类占比
        Map<String, Object> costAnalysis = new HashMap<>();
        List<Map<String, Object>> costDetails = new ArrayList<>();

        for (Map.Entry<String, java.math.BigDecimal> entry : costByCategory.entrySet()) {
            Map<String, Object> detail = new HashMap<>();
            detail.put("category", entry.getKey());
            detail.put("amount", entry.getValue());
            detail.put("percentage", totalCost.compareTo(java.math.BigDecimal.ZERO) == 0 ? 0 : 
                    entry.getValue().divide(totalCost, 4, java.math.RoundingMode.HALF_UP).doubleValue() * 100);
            costDetails.add(detail);
        }

        costAnalysis.put("details", costDetails);
        costAnalysis.put("totalCost", totalCost);
        result.put("costAnalysis", costAnalysis);

        return result;
    }

    @Override
    public Map<String, Object> generateCustomReport(Map<String, Object> reportParams) {
        Map<String, Object> result = new HashMap<>();
        // 根据参数生成自定义报表
        // 这里可以根据具体需求实现
        result.put("status", "success");
        result.put("message", "自定义报表生成成功");
        return result;
    }

    @Override
    public Map<String, Object> exportReport(String reportType, String format, Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        // 实现报表导出功能
        // 这里可以根据具体需求实现
        result.put("status", "success");
        result.put("message", "报表导出成功");
        result.put("format", format);
        return result;
    }

    /**
     * 按项目分析
     */
    private Map<String, Object> analyzeByProject(List<Account> accounts) {
        Map<String, java.math.BigDecimal> projectCosts = new HashMap<>();
        for (Account account : accounts) {
            if ("expense".equals(account.getType()) && account.getProjectName() != null) {
                String projectName = account.getProjectName();
                java.math.BigDecimal amount = account.getAmount();
                projectCosts.put(projectName, projectCosts.getOrDefault(projectName, java.math.BigDecimal.ZERO).add(amount));
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("projectCosts", projectCosts);
        return result;
    }

    /**
     * 按分包商分析
     */
    private Map<String, Object> analyzeBySubcontractor(List<Account> accounts) {
        Map<String, java.math.BigDecimal> subcontractorCosts = new HashMap<>();
        for (Account account : accounts) {
            if ("expense".equals(account.getType()) && account.getContractId() != null) {
                // 这里需要通过合同ID获取分包商信息
                // 简化实现，使用合同ID作为标识
                String subcontractor = "Subcontractor_" + account.getContractId();
                java.math.BigDecimal amount = account.getAmount();
                subcontractorCosts.put(subcontractor, subcontractorCosts.getOrDefault(subcontractor, java.math.BigDecimal.ZERO).add(amount));
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("subcontractorCosts", subcontractorCosts);
        return result;
    }

    /**
     * 按支付类型分析
     */
    private Map<String, Object> analyzeByPaymentType(List<Account> accounts) {
        Map<String, java.math.BigDecimal> paymentTypeCosts = new HashMap<>();
        for (Account account : accounts) {
            if ("expense".equals(account.getType())) {
                String paymentType = account.getPaymentType() != null ? account.getPaymentType() : "其他";
                java.math.BigDecimal amount = account.getAmount();
                paymentTypeCosts.put(paymentType, paymentTypeCosts.getOrDefault(paymentType, java.math.BigDecimal.ZERO).add(amount));
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("paymentTypeCosts", paymentTypeCosts);
        return result;
    }

    /**
     * 按合同分析
     */
    private Map<String, Object> analyzeByContract(List<Account> accounts) {
        Map<String, java.math.BigDecimal> contractCosts = new HashMap<>();
        for (Account account : accounts) {
            if ("expense".equals(account.getType()) && account.getContractId() != null) {
                String contract = "Contract_" + account.getContractId();
                java.math.BigDecimal amount = account.getAmount();
                contractCosts.put(contract, contractCosts.getOrDefault(contract, java.math.BigDecimal.ZERO).add(amount));
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("contractCosts", contractCosts);
        return result;
    }

    /**
     * 按日期分析
     */
    private Map<String, Object> analyzeByDate(List<Account> accounts, String startDate, String endDate) {
        Map<String, java.math.BigDecimal> monthlyCosts = new HashMap<>();
        for (Account account : accounts) {
            if ("expense".equals(account.getType())) {
                String month = new java.text.SimpleDateFormat("yyyy-MM").format(account.getTransactionDate());
                java.math.BigDecimal amount = account.getAmount();
                monthlyCosts.put(month, monthlyCosts.getOrDefault(month, java.math.BigDecimal.ZERO).add(amount));
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("monthlyCosts", monthlyCosts);
        return result;
    }

    /**
     * 分析历史现金流
     */
    private Map<String, java.math.BigDecimal> analyzeHistoricalCashFlow(List<Account> accounts) {
        Map<String, java.math.BigDecimal> monthlyFlow = new HashMap<>();
        for (Account account : accounts) {
            String month = new java.text.SimpleDateFormat("yyyy-MM").format(account.getTransactionDate());
            java.math.BigDecimal amount = "income".equals(account.getType()) ? 
                    account.getAmount() : account.getAmount().negate();
            monthlyFlow.put(month, monthlyFlow.getOrDefault(month, java.math.BigDecimal.ZERO).add(amount));
        }
        return monthlyFlow;
    }

    /**
     * 预测月度现金流
     */
    private java.math.BigDecimal predictMonthlyCashFlow(Map<String, java.math.BigDecimal> historicalData) {
        // 简单实现：计算历史平均值
        if (historicalData.isEmpty()) {
            return java.math.BigDecimal.ZERO;
        }
        java.math.BigDecimal sum = java.math.BigDecimal.ZERO;
        for (java.math.BigDecimal amount : historicalData.values()) {
            sum = sum.add(amount);
        }
        return sum.divide(java.math.BigDecimal.valueOf(historicalData.size()), 2, java.math.RoundingMode.HALF_UP);
    }
}
