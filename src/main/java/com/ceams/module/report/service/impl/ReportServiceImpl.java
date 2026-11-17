package com.ceams.module.report.service.impl;

import com.ceams.common.security.SecurityUtils;
import com.ceams.module.hr.service.HrEmployeeService;
import com.ceams.module.hr.service.HrPayrollService;
import com.ceams.module.hr.vo.HeadcountDeptVO;
import com.ceams.module.hr.vo.PayrollCostDeptVO;
import com.ceams.module.hr.vo.PayrollCostSummaryVO;
import com.ceams.module.report.dto.AdminAssetReportQuery;
import com.ceams.module.report.dto.BpmEfficiencyQuery;
import com.ceams.module.report.dto.FinancialReportQuery;
import com.ceams.module.report.dto.HrCostQuery;
import com.ceams.module.report.dto.HrPerformanceQuery;
import com.ceams.module.report.dto.MaterialReportQuery;
import com.ceams.module.report.dto.ProjectCostReportQuery;
import com.ceams.module.report.mapper.ReportMapper;
import com.ceams.module.report.service.ReportService;
import com.ceams.module.report.vo.AdminAssetCategoryRowVO;
import com.ceams.module.report.vo.AdminAssetCategoryStatVO;
import com.ceams.module.report.vo.AdminAssetReportVO;
import com.ceams.module.report.vo.AdminAssetStatsRowVO;
import com.ceams.module.report.vo.AdminBorrowStatsRowVO;
import com.ceams.module.report.vo.BpmBizTypeRowVO;
import com.ceams.module.report.vo.BpmBizTypeStatVO;
import com.ceams.module.report.vo.BpmEfficiencyReportVO;
import com.ceams.module.report.vo.BpmEfficiencySummaryRowVO;
import com.ceams.module.report.vo.BpmNodeBacklogVO;
import com.ceams.module.report.vo.DashboardVO;
import com.ceams.module.report.vo.FinancialReportItemVO;
import com.ceams.module.report.vo.FinancialReportVO;
import com.ceams.module.report.vo.HrCostDeptVO;
import com.ceams.module.report.vo.HrCostReportVO;
import com.ceams.module.report.vo.HrPerformanceDeptRowVO;
import com.ceams.module.report.vo.HrPerformanceDeptVO;
import com.ceams.module.report.vo.HrPerformanceReportVO;
import com.ceams.module.report.vo.MaterialReportItemVO;
import com.ceams.module.report.vo.MaterialReportVO;
import com.ceams.module.report.vo.ProjectCostItemVO;
import com.ceams.module.report.vo.ProjectCostReportVO;
import com.ceams.module.report.vo.ScoreDistributionVO;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
 * 跨域只读报表服务实现。
 *
 * <p>仅读、不开事务；多表聚合全部走 {@link ReportMapper} XML，人力成本复用 HR 域服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    /** 金额精度。 */
    private static final int MONEY_SCALE = 2;

    /** 比率精度。 */
    private static final int RATE_SCALE = 4;

    /** 秒 → 小时的换算基数。 */
    private static final BigDecimal SECONDS_PER_HOUR = BigDecimal.valueOf(3600);

    /** 未归属部门的展示名称。 */
    private static final String UNASSIGNED_DEPT_NAME = "未分配部门";

    private final ReportMapper reportMapper;
    private final HrPayrollService hrPayrollService;
    private final HrEmployeeService hrEmployeeService;

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

    // ------------------------------------------------------------------
    // P2 报表指标
    // ------------------------------------------------------------------

    @Override
    public HrCostReportVO hrCost(HrCostQuery query) {
        PayrollCostSummaryVO cost = hrPayrollService.costSummary(query.getMonth(), query.getDeptId());
        List<HeadcountDeptVO> headcounts = hrEmployeeService.headcountByDept(query.getDeptId());

        HrCostReportVO vo = new HrCostReportVO();
        vo.setMonth(query.getMonth());
        vo.setHeadcount(sumActiveHeadcount(headcounts));
        if (cost == null) {
            vo.setPayrollHeadcount(0L);
            normalizeCostAmounts(vo);
            return vo;
        }
        vo.setPayrollHeadcount(cost.getHeadcount());
        vo.setGrossPay(money(cost.getGrossPay()));
        vo.setNetPay(money(cost.getNetPay()));
        vo.setSocialInsurance(money(cost.getSocialInsurance()));
        vo.setHousingFund(money(cost.getHousingFund()));
        vo.setTax(money(cost.getTax()));
        vo.setAvgNetPay(money(cost.getAvgNetPay()));
        vo.setByDept(mergeCostDept(cost.getByDept(), headcounts));
        return vo;
    }

    @Override
    public HrPerformanceReportVO hrPerformance(HrPerformanceQuery query) {
        List<HrPerformanceDeptRowVO> rows =
                reportMapper.selectHrPerformanceByDept(query.getPeriod(), query.getDeptId());
        HrPerformanceReportVO vo = new HrPerformanceReportVO();
        vo.setPeriod(query.getPeriod());

        long reviewCount = 0L;
        long finalizedCount = 0L;
        BigDecimal sumFinalScore = BigDecimal.ZERO;
        ScoreDistributionVO distribution = new ScoreDistributionVO();
        List<HrPerformanceDeptVO> byDept = new ArrayList<>();
        if (rows != null) {
            for (HrPerformanceDeptRowVO row : rows) {
                if (row == null) {
                    continue;
                }
                long rowReview = nvl(row.getReviewCount());
                long rowFinalized = nvl(row.getFinalizedCount());
                BigDecimal rowSum = nvl(row.getSumFinalScore());
                reviewCount += rowReview;
                finalizedCount += rowFinalized;
                sumFinalScore = sumFinalScore.add(rowSum);
                distribution.setA(distribution.getA() + nvl(row.getBucketA()));
                distribution.setB(distribution.getB() + nvl(row.getBucketB()));
                distribution.setC(distribution.getC() + nvl(row.getBucketC()));
                distribution.setD(distribution.getD() + nvl(row.getBucketD()));

                HrPerformanceDeptVO dept = new HrPerformanceDeptVO();
                dept.setDeptId(row.getDeptId());
                dept.setDeptName(deptName(row.getDeptName(), row.getDeptId()));
                dept.setReviewCount(rowReview);
                dept.setFinalizedCount(rowFinalized);
                dept.setAvgFinalScore(average(rowSum, rowFinalized));
                byDept.add(dept);
            }
        }
        byDept.sort(
                Comparator.comparing(
                        HrPerformanceDeptVO::getDeptId, Comparator.nullsLast(Comparator.naturalOrder())));

        vo.setReviewCount(reviewCount);
        vo.setFinalizedCount(finalizedCount);
        vo.setAvgFinalScore(average(sumFinalScore, finalizedCount));
        vo.setDistribution(distribution);
        vo.setByDept(byDept);
        return vo;
    }

    @Override
    public AdminAssetReportVO adminAssets(AdminAssetReportQuery query) {
        String category = query == null ? null : query.getCategory();
        AdminAssetStatsRowVO stats = reportMapper.selectAdminAssetStats(category);
        AdminAssetReportVO vo = new AdminAssetReportVO();

        long totalCount = stats == null ? 0L : nvl(stats.getTotalCount());
        vo.setTotalCount(totalCount);
        vo.setTotalValue(stats == null ? money(BigDecimal.ZERO) : money(stats.getTotalValue()));

        Map<String, Long> byStatus = new LinkedHashMap<>();
        byStatus.put("IN_STOCK", stats == null ? 0L : nvl(stats.getInStockCount()));
        byStatus.put("BORROWED", stats == null ? 0L : nvl(stats.getBorrowedCount()));
        byStatus.put("REPAIRING", stats == null ? 0L : nvl(stats.getRepairingCount()));
        byStatus.put("SCRAPPED", stats == null ? 0L : nvl(stats.getScrappedCount()));
        vo.setByStatus(byStatus);

        List<AdminAssetCategoryRowVO> categoryRows =
                reportMapper.selectAdminAssetByCategory(category);
        List<AdminAssetCategoryStatVO> byCategory = new ArrayList<>();
        if (categoryRows != null) {
            for (AdminAssetCategoryRowVO row : categoryRows) {
                if (row == null) {
                    continue;
                }
                AdminAssetCategoryStatVO stat = new AdminAssetCategoryStatVO();
                stat.setCategory(row.getCategory());
                stat.setCount(nvl(row.getCount()));
                stat.setTotalValue(money(row.getTotalValue()));
                byCategory.add(stat);
            }
        }
        vo.setByCategory(byCategory);

        AdminBorrowStatsRowVO borrowStats = reportMapper.selectAdminBorrowStats(category);
        long borrowingCount = borrowStats == null ? 0L : nvl(borrowStats.getBorrowingCount());
        vo.setBorrowingCount(borrowingCount);
        vo.setOverdueCount(borrowStats == null ? 0L : nvl(borrowStats.getOverdueCount()));
        vo.setBorrowRate(ratio(borrowingCount, totalCount));
        return vo;
    }

    @Override
    public BpmEfficiencyReportVO bpmEfficiency(BpmEfficiencyQuery query) {
        String start = query == null ? null : query.getStart();
        String end = query == null ? null : query.getEnd();
        String bizType = query == null ? null : query.getBizType();

        BpmEfficiencySummaryRowVO summary =
                reportMapper.selectBpmEfficiencySummary(start, end, bizType);
        BpmEfficiencyReportVO vo = new BpmEfficiencyReportVO();
        long taskCount = summary == null ? 0L : nvl(summary.getTaskCount());
        long overdueTaskCount = summary == null ? 0L : nvl(summary.getOverdueTaskCount());
        vo.setInstanceCount(summary == null ? 0L : nvl(summary.getInstanceCount()));
        vo.setApprovedCount(summary == null ? 0L : nvl(summary.getApprovedCount()));
        vo.setAvgDurationHours(
                hours(summary == null ? null : summary.getAvgDurationSeconds()));
        vo.setOverdueTaskCount(overdueTaskCount);
        vo.setOverdueRate(ratio(overdueTaskCount, taskCount));

        List<BpmBizTypeRowVO> bizTypeRows =
                reportMapper.selectBpmEfficiencyByBizType(start, end, bizType);
        List<BpmBizTypeStatVO> byBizType = new ArrayList<>();
        if (bizTypeRows != null) {
            for (BpmBizTypeRowVO row : bizTypeRows) {
                if (row == null) {
                    continue;
                }
                BpmBizTypeStatVO stat = new BpmBizTypeStatVO();
                stat.setBizType(row.getBizType());
                stat.setInstanceCount(nvl(row.getInstanceCount()));
                stat.setApprovedCount(nvl(row.getApprovedCount()));
                stat.setAvgDurationHours(hours(row.getAvgDurationSeconds()));
                byBizType.add(stat);
            }
        }
        vo.setByBizType(byBizType);

        List<BpmNodeBacklogVO> backlogRows =
                reportMapper.selectBpmBacklogByNode(start, end, bizType);
        List<BpmNodeBacklogVO> backlog = new ArrayList<>();
        if (backlogRows != null) {
            for (BpmNodeBacklogVO row : backlogRows) {
                if (row != null) {
                    backlog.add(row);
                }
            }
        }
        vo.setBacklogByNode(backlog);
        return vo;
    }

    // ------------------------------------------------------------------
    // 内部工具
    // ------------------------------------------------------------------

    /** 在职人数 = Σ(totalCount - terminatedCount)，含试用与休假。 */
    private long sumActiveHeadcount(List<HeadcountDeptVO> headcounts) {
        long headcount = 0L;
        if (headcounts == null) {
            return headcount;
        }
        for (HeadcountDeptVO item : headcounts) {
            if (item != null) {
                headcount += item.getTotalCount() - item.getTerminatedCount();
            }
        }
        return headcount;
    }

    /**
     * 合并工资口径与员工口径的部门明细：
     * 以 deptId 为键求并集，任一侧缺失的金额 / 人数补 0，部门名称优先取员工口径。
     */
    private List<HrCostDeptVO> mergeCostDept(
            List<PayrollCostDeptVO> payrollDepts, List<HeadcountDeptVO> headcounts) {
        Map<Long, HrCostDeptVO> merged = new LinkedHashMap<>();
        if (payrollDepts != null) {
            for (PayrollCostDeptVO row : payrollDepts) {
                if (row == null) {
                    continue;
                }
                HrCostDeptVO line = merged.computeIfAbsent(row.getDeptId(), ReportServiceImpl::newDeptLine);
                if (StringUtils.isNotBlank(row.getDeptName())) {
                    line.setDeptName(row.getDeptName());
                }
                line.setPayrollHeadcount(row.getHeadcount());
                line.setGrossPay(money(row.getGrossPay()));
                line.setNetPay(money(row.getNetPay()));
                line.setSocialInsurance(money(row.getSocialInsurance()));
                line.setHousingFund(money(row.getHousingFund()));
                line.setTax(money(row.getTax()));
                line.setAvgNetPay(money(row.getAvgNetPay()));
            }
        }
        if (headcounts != null) {
            for (HeadcountDeptVO item : headcounts) {
                if (item == null) {
                    continue;
                }
                HrCostDeptVO line =
                        merged.computeIfAbsent(item.getDeptId(), ReportServiceImpl::newDeptLine);
                if (StringUtils.isNotBlank(item.getDeptName())) {
                    line.setDeptName(item.getDeptName());
                }
                line.setHeadcount(item.getTotalCount() - item.getTerminatedCount());
            }
        }
        List<HrCostDeptVO> result = new ArrayList<>(merged.values());
        for (HrCostDeptVO line : result) {
            if (StringUtils.isBlank(line.getDeptName())) {
                line.setDeptName(
                        line.getDeptId() == null ? UNASSIGNED_DEPT_NAME : StringUtils.EMPTY);
            }
            // 未命中侧统一补 0（保留 2 位小数，避免前端出现 null / 0 精度不一致）
            line.setGrossPay(money(line.getGrossPay()));
            line.setNetPay(money(line.getNetPay()));
            line.setSocialInsurance(money(line.getSocialInsurance()));
            line.setHousingFund(money(line.getHousingFund()));
            line.setTax(money(line.getTax()));
            line.setAvgNetPay(money(line.getAvgNetPay()));
        }
        result.sort(
                Comparator.comparing(
                        HrCostDeptVO::getDeptId, Comparator.nullsLast(Comparator.naturalOrder())));
        return result;
    }

    private static HrCostDeptVO newDeptLine(Long deptId) {
        HrCostDeptVO line = new HrCostDeptVO();
        line.setDeptId(deptId);
        return line;
    }

    /** 部门名称兜底：deptId 为空时归入「未分配部门」。 */
    private String deptName(String name, Long deptId) {
        if (StringUtils.isNotBlank(name)) {
            return name;
        }
        return deptId == null ? UNASSIGNED_DEPT_NAME : StringUtils.EMPTY;
    }

    /** 平均分：被除数为 0 时返回 0.00。 */
    private BigDecimal average(BigDecimal total, long count) {
        if (count == 0L) {
            return money(BigDecimal.ZERO);
        }
        return total.divide(BigDecimal.valueOf(count), MONEY_SCALE, RoundingMode.HALF_UP);
    }

    /** 秒 → 小时，保留 2 位小数。 */
    private BigDecimal hours(BigDecimal seconds) {
        if (seconds == null) {
            return money(BigDecimal.ZERO);
        }
        return seconds
                .divide(SECONDS_PER_HOUR, MONEY_SCALE + 2, RoundingMode.HALF_UP)
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    /** 比率：分母为 0 时返回 0.0000。 */
    private BigDecimal ratio(long numerator, long denominator) {
        if (denominator == 0L) {
            return BigDecimal.ZERO.setScale(RATE_SCALE, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(numerator)
                .divide(BigDecimal.valueOf(denominator), RATE_SCALE, RoundingMode.HALF_UP);
    }

    private void normalizeCostAmounts(HrCostReportVO vo) {
        vo.setGrossPay(money(BigDecimal.ZERO));
        vo.setNetPay(money(BigDecimal.ZERO));
        vo.setSocialInsurance(money(BigDecimal.ZERO));
        vo.setHousingFund(money(BigDecimal.ZERO));
        vo.setTax(money(BigDecimal.ZERO));
        vo.setAvgNetPay(money(BigDecimal.ZERO));
    }

    private long nvl(Long value) {
        return value == null ? 0L : value;
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal money(BigDecimal value) {
        return nvl(value).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
