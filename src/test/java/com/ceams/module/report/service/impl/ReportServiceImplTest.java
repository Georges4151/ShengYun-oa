package com.ceams.module.report.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

import com.ceams.module.hr.service.HrEmployeeService;
import com.ceams.module.hr.service.HrPayrollService;
import com.ceams.module.hr.vo.HeadcountDeptVO;
import com.ceams.module.hr.vo.PayrollCostDeptVO;
import com.ceams.module.hr.vo.PayrollCostSummaryVO;
import com.ceams.module.report.dto.AdminAssetReportQuery;
import com.ceams.module.report.dto.BpmEfficiencyQuery;
import com.ceams.module.report.dto.HrCostQuery;
import com.ceams.module.report.dto.HrPerformanceQuery;
import com.ceams.module.report.mapper.ReportMapper;
import com.ceams.module.report.vo.AdminAssetCategoryRowVO;
import com.ceams.module.report.vo.AdminAssetReportVO;
import com.ceams.module.report.vo.AdminAssetStatsRowVO;
import com.ceams.module.report.vo.AdminBorrowStatsRowVO;
import com.ceams.module.report.vo.BpmBizTypeRowVO;
import com.ceams.module.report.vo.BpmEfficiencyReportVO;
import com.ceams.module.report.vo.BpmEfficiencySummaryRowVO;
import com.ceams.module.report.vo.BpmNodeBacklogVO;
import com.ceams.module.report.vo.HrCostDeptVO;
import com.ceams.module.report.vo.HrCostReportVO;
import com.ceams.module.report.vo.HrPerformanceDeptRowVO;
import com.ceams.module.report.vo.HrPerformanceReportVO;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** 报表指标服务单元测试：空数据兜底与正常聚合口径。 */
@ExtendWith(MockitoExtension.class)
class ReportServiceImplTest {

    @Mock private ReportMapper reportMapper;
    @Mock private HrPayrollService hrPayrollService;
    @Mock private HrEmployeeService hrEmployeeService;

    private ReportServiceImpl reportService;

    @BeforeEach
    void setUp() {
        reportService = new ReportServiceImpl(reportMapper, hrPayrollService, hrEmployeeService);
    }

    // ------------------------------------------------------------------
    // 人力成本
    // ------------------------------------------------------------------

    @Test
    void hrCostShouldReturnZeroWhenNoData() {
        when(hrPayrollService.costSummary("2026-11", null))
                .thenReturn(new PayrollCostSummaryVO());
        when(hrEmployeeService.headcountByDept(null)).thenReturn(null);

        HrCostReportVO vo = reportService.hrCost(monthQuery("2026-11", null));

        assertThat(vo.getMonth()).isEqualTo("2026-11");
        assertThat(vo.getHeadcount()).isZero();
        assertThat(vo.getPayrollHeadcount()).isZero();
        assertThat(vo.getGrossPay()).isEqualByComparingTo("0.00");
        assertThat(vo.getNetPay()).isEqualByComparingTo("0.00");
        assertThat(vo.getSocialInsurance()).isEqualByComparingTo("0.00");
        assertThat(vo.getHousingFund()).isEqualByComparingTo("0.00");
        assertThat(vo.getTax()).isEqualByComparingTo("0.00");
        assertThat(vo.getAvgNetPay()).isEqualByComparingTo("0.00");
        assertThat(vo.getByDept()).isEmpty();
    }

    @Test
    void hrCostShouldMergePayrollAndHeadcountByDept() {
        PayrollCostSummaryVO cost = new PayrollCostSummaryVO();
        cost.setHeadcount(2L);
        cost.setGrossPay(new BigDecimal("30000"));
        cost.setNetPay(new BigDecimal("24000"));
        cost.setSocialInsurance(new BigDecimal("3000"));
        cost.setHousingFund(new BigDecimal("1500"));
        cost.setTax(new BigDecimal("900"));
        cost.setAvgNetPay(new BigDecimal("12000"));
        List<PayrollCostDeptVO> payrollDepts = new ArrayList<>();
        payrollDepts.add(payrollDept(8L, "旧部门名", 1L, "20000", "16000"));
        payrollDepts.add(payrollDept(null, "未分配部门", 1L, "10000", "8000"));
        cost.setByDept(payrollDepts);

        when(hrPayrollService.costSummary("2026-11", null)).thenReturn(cost);
        when(hrEmployeeService.headcountByDept(null))
                .thenReturn(
                        List.of(
                                headcount(8L, "研发部", 10L, 2L),
                                headcount(9L, "行政部", 4L, 0L)));

        HrCostReportVO vo = reportService.hrCost(monthQuery("2026-11", null));

        assertThat(vo.getHeadcount()).isEqualTo(12L);
        assertThat(vo.getPayrollHeadcount()).isEqualTo(2L);
        assertThat(vo.getByDept()).hasSize(3);
        // deptId 升序，未分配部门（deptId 为空）排在最后
        HrCostDeptVO dept8 = vo.getByDept().get(0);
        assertThat(dept8.getDeptId()).isEqualTo(8L);
        assertThat(dept8.getDeptName()).isEqualTo("研发部");
        assertThat(dept8.getHeadcount()).isEqualTo(8L);
        assertThat(dept8.getPayrollHeadcount()).isEqualTo(1L);
        assertThat(dept8.getGrossPay()).isEqualByComparingTo("20000.00");
        assertThat(dept8.getNetPay()).isEqualByComparingTo("16000.00");
        assertThat(dept8.getAvgNetPay()).isEqualByComparingTo("16000.00");

        HrCostDeptVO dept9 = vo.getByDept().get(1);
        assertThat(dept9.getDeptId()).isEqualTo(9L);
        assertThat(dept9.getDeptName()).isEqualTo("行政部");
        assertThat(dept9.getHeadcount()).isEqualTo(4L);
        assertThat(dept9.getPayrollHeadcount()).isZero();
        assertThat(dept9.getGrossPay()).isEqualByComparingTo("0.00");
        assertThat(dept9.getNetPay()).isEqualByComparingTo("0.00");

        HrCostDeptVO unassigned = vo.getByDept().get(2);
        assertThat(unassigned.getDeptId()).isNull();
        assertThat(unassigned.getDeptName()).isEqualTo("未分配部门");
        assertThat(unassigned.getHeadcount()).isZero();
        assertThat(unassigned.getPayrollHeadcount()).isEqualTo(1L);
        assertThat(unassigned.getNetPay()).isEqualByComparingTo("8000.00");
    }

    // ------------------------------------------------------------------
    // 绩效考核
    // ------------------------------------------------------------------

    @Test
    void hrPerformanceShouldReturnZeroWhenNoData() {
        when(reportMapper.selectHrPerformanceByDept("2026-Q4", null)).thenReturn(List.of());

        HrPerformanceReportVO vo =
                reportService.hrPerformance(performanceQuery("2026-Q4", null));

        assertThat(vo.getPeriod()).isEqualTo("2026-Q4");
        assertThat(vo.getReviewCount()).isZero();
        assertThat(vo.getFinalizedCount()).isZero();
        assertThat(vo.getAvgFinalScore()).isEqualByComparingTo("0.00");
        assertThat(vo.getDistribution().getA()).isZero();
        assertThat(vo.getDistribution().getB()).isZero();
        assertThat(vo.getDistribution().getC()).isZero();
        assertThat(vo.getDistribution().getD()).isZero();
        assertThat(vo.getByDept()).isEmpty();
    }

    @Test
    void hrPerformanceShouldAggregateScoreAndDistribution() {
        when(reportMapper.selectHrPerformanceByDept("2026-11", null))
                .thenReturn(
                        List.of(
                                performanceRow(8L, "研发部", 3L, 2L, "180.00", 1L, 1L, 0L, 0L),
                                performanceRow(null, null, 2L, 1L, "55.50", 0L, 0L, 1L, 0L)));

        HrPerformanceReportVO vo =
                reportService.hrPerformance(performanceQuery("2026-11", null));

        assertThat(vo.getReviewCount()).isEqualTo(5L);
        assertThat(vo.getFinalizedCount()).isEqualTo(3L);
        // (180.00 + 55.50) / 3 = 78.50
        assertThat(vo.getAvgFinalScore()).isEqualByComparingTo("78.50");
        assertThat(vo.getAvgFinalScore().scale()).isEqualTo(2);
        assertThat(vo.getDistribution().getA()).isEqualTo(1L);
        assertThat(vo.getDistribution().getB()).isEqualTo(1L);
        assertThat(vo.getDistribution().getC()).isEqualTo(1L);
        assertThat(vo.getDistribution().getD()).isZero();

        assertThat(vo.getByDept()).hasSize(2);
        assertThat(vo.getByDept().get(0).getDeptName()).isEqualTo("研发部");
        assertThat(vo.getByDept().get(0).getAvgFinalScore()).isEqualByComparingTo("90.00");
        assertThat(vo.getByDept().get(1).getDeptId()).isNull();
        assertThat(vo.getByDept().get(1).getDeptName()).isEqualTo("未分配部门");
        assertThat(vo.getByDept().get(1).getAvgFinalScore()).isEqualByComparingTo("55.50");
    }

    // ------------------------------------------------------------------
    // 行政资产
    // ------------------------------------------------------------------

    @Test
    void adminAssetsShouldReturnZeroWhenNoData() {
        when(reportMapper.selectAdminAssetStats(isNull())).thenReturn(null);
        when(reportMapper.selectAdminAssetByCategory(isNull())).thenReturn(List.of());
        when(reportMapper.selectAdminBorrowStats(isNull())).thenReturn(null);

        AdminAssetReportVO vo = reportService.adminAssets(new AdminAssetReportQuery());

        assertThat(vo.getTotalCount()).isZero();
        assertThat(vo.getTotalValue()).isEqualByComparingTo("0.00");
        assertThat(vo.getBorrowingCount()).isZero();
        assertThat(vo.getOverdueCount()).isZero();
        assertThat(vo.getBorrowRate()).isEqualByComparingTo("0.0000");
        assertThat(vo.getBorrowRate().scale()).isEqualTo(4);
        assertThat(vo.getByStatus())
                .containsExactly(
                        entry("IN_STOCK", 0L),
                        entry("BORROWED", 0L),
                        entry("REPAIRING", 0L),
                        entry("SCRAPPED", 0L));
        assertThat(vo.getByCategory()).isEmpty();
    }

    @Test
    void adminAssetsShouldComputeStatusAndBorrowRate() {
        AdminAssetStatsRowVO stats = new AdminAssetStatsRowVO();
        stats.setTotalCount(10L);
        stats.setTotalValue(new BigDecimal("12345.6"));
        stats.setInStockCount(4L);
        stats.setBorrowedCount(3L);
        stats.setRepairingCount(2L);
        stats.setScrappedCount(1L);
        when(reportMapper.selectAdminAssetStats(isNull())).thenReturn(stats);

        AdminAssetCategoryRowVO electronic = new AdminAssetCategoryRowVO();
        electronic.setCategory("ELECTRONIC");
        electronic.setCount(6L);
        electronic.setTotalValue(new BigDecimal("8000.10"));
        AdminAssetCategoryRowVO furniture = new AdminAssetCategoryRowVO();
        furniture.setCategory("FURNITURE");
        furniture.setCount(4L);
        furniture.setTotalValue(new BigDecimal("4345.50"));
        when(reportMapper.selectAdminAssetByCategory(isNull()))
                .thenReturn(List.of(electronic, furniture));

        AdminBorrowStatsRowVO borrow = new AdminBorrowStatsRowVO();
        borrow.setBorrowingCount(3L);
        borrow.setOverdueCount(2L);
        when(reportMapper.selectAdminBorrowStats(isNull())).thenReturn(borrow);

        AdminAssetReportVO vo = reportService.adminAssets(new AdminAssetReportQuery());

        assertThat(vo.getTotalCount()).isEqualTo(10L);
        assertThat(vo.getTotalValue()).isEqualByComparingTo("12345.60");
        assertThat(vo.getByStatus().get("IN_STOCK")).isEqualTo(4L);
        assertThat(vo.getByStatus().get("BORROWED")).isEqualTo(3L);
        assertThat(vo.getByStatus().get("REPAIRING")).isEqualTo(2L);
        assertThat(vo.getByStatus().get("SCRAPPED")).isEqualTo(1L);
        assertThat(vo.getByCategory()).hasSize(2);
        assertThat(vo.getByCategory().get(0).getCategory()).isEqualTo("ELECTRONIC");
        assertThat(vo.getByCategory().get(0).getTotalValue()).isEqualByComparingTo("8000.10");
        assertThat(vo.getBorrowingCount()).isEqualTo(3L);
        assertThat(vo.getOverdueCount()).isEqualTo(2L);
        // 3 / 10 = 0.3000
        assertThat(vo.getBorrowRate()).isEqualByComparingTo("0.3000");
        assertThat(vo.getBorrowRate().scale()).isEqualTo(4);
    }

    // ------------------------------------------------------------------
    // 审批效率
    // ------------------------------------------------------------------

    @Test
    void bpmEfficiencyShouldReturnZeroWhenNoData() {
        when(reportMapper.selectBpmEfficiencySummary(null, null, null)).thenReturn(null);
        when(reportMapper.selectBpmEfficiencyByBizType(null, null, null)).thenReturn(List.of());
        when(reportMapper.selectBpmBacklogByNode(null, null, null)).thenReturn(List.of());

        BpmEfficiencyReportVO vo = reportService.bpmEfficiency(new BpmEfficiencyQuery());

        assertThat(vo.getInstanceCount()).isZero();
        assertThat(vo.getApprovedCount()).isZero();
        assertThat(vo.getAvgDurationHours()).isEqualByComparingTo("0.00");
        assertThat(vo.getOverdueTaskCount()).isZero();
        assertThat(vo.getOverdueRate()).isEqualByComparingTo("0.0000");
        assertThat(vo.getOverdueRate().scale()).isEqualTo(4);
        assertThat(vo.getByBizType()).isEmpty();
        assertThat(vo.getBacklogByNode()).isEmpty();
    }

    @Test
    void bpmEfficiencyShouldConvertDurationAndComputeOverdueRate() {
        BpmEfficiencySummaryRowVO summary = new BpmEfficiencySummaryRowVO();
        summary.setInstanceCount(4L);
        summary.setApprovedCount(3L);
        // 5400 秒 = 1.5 小时
        summary.setAvgDurationSeconds(new BigDecimal("5400"));
        summary.setTaskCount(10L);
        summary.setOverdueTaskCount(2L);
        when(reportMapper.selectBpmEfficiencySummary("2026-11-01", "2026-11-30", "EXPENSE"))
                .thenReturn(summary);

        BpmBizTypeRowVO bizType = new BpmBizTypeRowVO();
        bizType.setBizType("EXPENSE");
        bizType.setInstanceCount(4L);
        bizType.setApprovedCount(3L);
        bizType.setAvgDurationSeconds(new BigDecimal("3600"));
        when(reportMapper.selectBpmEfficiencyByBizType("2026-11-01", "2026-11-30", "EXPENSE"))
                .thenReturn(List.of(bizType));

        BpmNodeBacklogVO backlog = new BpmNodeBacklogVO();
        backlog.setNodeKey("LEADER_APPROVE");
        backlog.setPendingCount(3L);
        when(reportMapper.selectBpmBacklogByNode("2026-11-01", "2026-11-30", "EXPENSE"))
                .thenReturn(List.of(backlog));

        BpmEfficiencyReportVO vo =
                reportService.bpmEfficiency(bpmQuery("2026-11-01", "2026-11-30", "EXPENSE"));

        assertThat(vo.getInstanceCount()).isEqualTo(4L);
        assertThat(vo.getApprovedCount()).isEqualTo(3L);
        assertThat(vo.getAvgDurationHours()).isEqualByComparingTo("1.50");
        assertThat(vo.getOverdueTaskCount()).isEqualTo(2L);
        // 2 / 10 = 0.2000
        assertThat(vo.getOverdueRate()).isEqualByComparingTo("0.2000");
        assertThat(vo.getByBizType()).hasSize(1);
        assertThat(vo.getByBizType().get(0).getBizType()).isEqualTo("EXPENSE");
        assertThat(vo.getByBizType().get(0).getAvgDurationHours()).isEqualByComparingTo("1.00");
        assertThat(vo.getBacklogByNode()).hasSize(1);
        assertThat(vo.getBacklogByNode().get(0).getNodeKey()).isEqualTo("LEADER_APPROVE");
        assertThat(vo.getBacklogByNode().get(0).getPendingCount()).isEqualTo(3L);
    }

    // ------------------------------------------------------------------
    // 测试数据构造
    // ------------------------------------------------------------------

    private HrCostQuery monthQuery(String month, Long deptId) {
        HrCostQuery query = new HrCostQuery();
        query.setMonth(month);
        query.setDeptId(deptId);
        return query;
    }

    private HrPerformanceQuery performanceQuery(String period, Long deptId) {
        HrPerformanceQuery query = new HrPerformanceQuery();
        query.setPeriod(period);
        query.setDeptId(deptId);
        return query;
    }

    private BpmEfficiencyQuery bpmQuery(String start, String end, String bizType) {
        BpmEfficiencyQuery query = new BpmEfficiencyQuery();
        query.setStart(start);
        query.setEnd(end);
        query.setBizType(bizType);
        return query;
    }

    private PayrollCostDeptVO payrollDept(
            Long deptId, String deptName, long headcount, String grossPay, String netPay) {
        PayrollCostDeptVO vo = new PayrollCostDeptVO();
        vo.setDeptId(deptId);
        vo.setDeptName(deptName);
        vo.setHeadcount(headcount);
        vo.setGrossPay(new BigDecimal(grossPay));
        vo.setNetPay(new BigDecimal(netPay));
        vo.setAvgNetPay(new BigDecimal(netPay));
        return vo;
    }

    private HeadcountDeptVO headcount(
            Long deptId, String deptName, long totalCount, long terminatedCount) {
        HeadcountDeptVO vo = new HeadcountDeptVO();
        vo.setDeptId(deptId);
        vo.setDeptName(deptName);
        vo.setTotalCount(totalCount);
        vo.setTerminatedCount(terminatedCount);
        return vo;
    }

    private HrPerformanceDeptRowVO performanceRow(
            Long deptId,
            String deptName,
            long reviewCount,
            long finalizedCount,
            String sumFinalScore,
            long bucketA,
            long bucketB,
            long bucketC,
            long bucketD) {
        HrPerformanceDeptRowVO row = new HrPerformanceDeptRowVO();
        row.setDeptId(deptId);
        row.setDeptName(deptName);
        row.setReviewCount(reviewCount);
        row.setFinalizedCount(finalizedCount);
        row.setSumFinalScore(new BigDecimal(sumFinalScore));
        row.setBucketA(bucketA);
        row.setBucketB(bucketB);
        row.setBucketC(bucketC);
        row.setBucketD(bucketD);
        return row;
    }
}
