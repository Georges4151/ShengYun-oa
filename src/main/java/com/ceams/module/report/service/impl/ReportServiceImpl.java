package com.ceams.module.report.service.impl;

import com.ceams.common.security.SecurityUtils;
import com.ceams.module.report.dto.FinancialReportQuery;
import com.ceams.module.report.dto.MaterialReportQuery;
import com.ceams.module.report.dto.ProjectCostReportQuery;
import com.ceams.module.report.mapper.ReportMapper;
import com.ceams.module.report.service.ReportService;
import com.ceams.module.report.vo.DashboardVO;
import com.ceams.module.report.vo.FinancialReportItemVO;
import com.ceams.module.report.vo.FinancialReportVO;
import com.ceams.module.report.vo.MaterialReportItemVO;
import com.ceams.module.report.vo.MaterialReportVO;
import com.ceams.module.report.vo.ProjectCostItemVO;
import com.ceams.module.report.vo.ProjectCostReportVO;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 跨域只读报表服务实现。
 *
 * <p>仅读、不开事务；多表聚合全部走 {@link ReportMapper} XML。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    private final ReportMapper reportMapper;

    @Override
    public FinancialReportVO financial(FinancialReportQuery query) {
        List<FinancialReportItemVO> items =
                reportMapper.selectFinancialItems(
                        query.getMonth(), query.getProjectId(), query.getAccountId());
        BigDecimal income = BigDecimal.ZERO;
        BigDecimal expense = BigDecimal.ZERO;
        for (FinancialReportItemVO item : items) {
            BigDecimal amount = nvl(item.getAmount());
            if ("INCOME".equals(item.getType())) {
                income = income.add(amount);
            } else if ("EXPENSE".equals(item.getType())) {
                expense = expense.add(amount);
            }
        }
        FinancialReportVO vo = new FinancialReportVO();
        vo.setItems(items == null ? Collections.emptyList() : items);
        vo.setTotalIncome(income);
        vo.setTotalExpense(expense);
        vo.setNetAmount(income.subtract(expense));
        return vo;
    }

    @Override
    public ProjectCostReportVO projectCost(ProjectCostReportQuery query) {
        List<ProjectCostItemVO> items = reportMapper.selectProjectCostItems(query.getProjectId());
        BigDecimal totalBudget = BigDecimal.ZERO;
        BigDecimal totalChanged = BigDecimal.ZERO;
        BigDecimal totalPaid = BigDecimal.ZERO;
        BigDecimal totalAllocated = BigDecimal.ZERO;
        if (items != null) {
            for (ProjectCostItemVO item : items) {
                totalBudget = totalBudget.add(nvl(item.getBudgetAmount()));
                totalChanged = totalChanged.add(nvl(item.getChangedAmount()));
                totalPaid = totalPaid.add(nvl(item.getPaidAmount()));
                totalAllocated = totalAllocated.add(nvl(item.getAllocatedAmount()));
            }
        }
        ProjectCostReportVO vo = new ProjectCostReportVO();
        vo.setItems(items == null ? Collections.emptyList() : items);
        vo.setTotalBudget(totalBudget);
        vo.setTotalChanged(totalChanged);
        vo.setTotalPaid(totalPaid);
        vo.setTotalAllocated(totalAllocated);
        return vo;
    }

    @Override
    public MaterialReportVO material(MaterialReportQuery query) {
        List<MaterialReportItemVO> items =
                reportMapper.selectMaterialItems(query.getMaterialId(), query.getCategoryId());
        BigDecimal totalAmount = BigDecimal.ZERO;
        if (items != null) {
            for (MaterialReportItemVO item : items) {
                totalAmount = totalAmount.add(nvl(item.getTotalAmount()));
            }
        }
        MaterialReportVO vo = new MaterialReportVO();
        vo.setItems(items == null ? Collections.emptyList() : items);
        vo.setTotalCount(items == null ? 0L : (long) items.size());
        vo.setTotalAmount(totalAmount);
        return vo;
    }

    @Override
    public DashboardVO dashboard() {
        Long userId = SecurityUtils.getCurrentUserIdOrDefault();
        String month = YearMonth.now().format(MONTH_FORMATTER);
        DashboardVO vo = reportMapper.selectDashboard(userId, month);
        return vo == null ? new DashboardVO() : vo;
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
