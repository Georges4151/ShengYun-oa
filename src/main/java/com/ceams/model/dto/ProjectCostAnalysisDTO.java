package com.ceams.model.dto;

import java.math.BigDecimal;

/**
 * 项目成本分析数据传输对象
 */
public class ProjectCostAnalysisDTO {
    private Integer projectId;
    private String projectName;
    private BigDecimal contractAmount;
    private BigDecimal budgetAmount;
    private BigDecimal actualAmount;
    private BigDecimal budgetSurplus;
    private Double costOverrunRate;

    // 构造方法
    public ProjectCostAnalysisDTO(Integer projectId, String projectName, BigDecimal contractAmount,
            BigDecimal budgetAmount, BigDecimal actualAmount,
            BigDecimal budgetSurplus, Double costOverrunRate) {
        this.projectId = projectId;
        this.projectName = projectName;
        this.contractAmount = contractAmount;
        this.budgetAmount = budgetAmount;
        this.actualAmount = actualAmount;
        this.budgetSurplus = budgetSurplus;
        this.costOverrunRate = costOverrunRate;
    }

    // Getter and Setter methods
    public Integer getProjectId() {
        return projectId;
    }

    public void setProjectId(Integer projectId) {
        this.projectId = projectId;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public BigDecimal getContractAmount() {
        return contractAmount;
    }

    public void setContractAmount(BigDecimal contractAmount) {
        this.contractAmount = contractAmount;
    }

    public BigDecimal getBudgetAmount() {
        return budgetAmount;
    }

    public void setBudgetAmount(BigDecimal budgetAmount) {
        this.budgetAmount = budgetAmount;
    }

    public BigDecimal getActualAmount() {
        return actualAmount;
    }

    public void setActualAmount(BigDecimal actualAmount) {
        this.actualAmount = actualAmount;
    }

    public BigDecimal getBudgetSurplus() {
        return budgetSurplus;
    }

    public void setBudgetSurplus(BigDecimal budgetSurplus) {
        this.budgetSurplus = budgetSurplus;
    }

    public Double getCostOverrunRate() {
        return costOverrunRate;
    }

    public void setCostOverrunRate(Double costOverrunRate) {
        this.costOverrunRate = costOverrunRate;
    }
}
