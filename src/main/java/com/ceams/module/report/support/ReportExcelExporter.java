package com.ceams.module.report.support;

import com.ceams.module.hr.support.SimpleXlsxWriter;
import com.ceams.module.report.vo.AdminAssetCategoryStatVO;
import com.ceams.module.report.vo.AdminAssetReportVO;
import com.ceams.module.report.vo.BpmBizTypeStatVO;
import com.ceams.module.report.vo.BpmEfficiencyReportVO;
import com.ceams.module.report.vo.BpmNodeBacklogVO;
import com.ceams.module.report.vo.FinancialReportItemVO;
import com.ceams.module.report.vo.FinancialReportVO;
import com.ceams.module.report.vo.HrCostDeptVO;
import com.ceams.module.report.vo.HrCostReportVO;
import com.ceams.module.report.vo.HrPerformanceDeptVO;
import com.ceams.module.report.vo.HrPerformanceReportVO;
import com.ceams.module.report.vo.MaterialReportItemVO;
import com.ceams.module.report.vo.MaterialReportVO;
import com.ceams.module.report.vo.ProjectCostItemVO;
import com.ceams.module.report.vo.ProjectCostReportVO;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * 报表 Excel 导出：复用 {@link SimpleXlsxWriter} 生成单表 xlsx。
 *
 * <p>每个方法输出「表头 + 明细行 + 合计行」，字段口径与报表页面列一致，
 * 不在导出侧做二次聚合，避免与页面数据不一致。
 */
@Component
public class ReportExcelExporter {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 财务流水来源。 */
    private static final Map<String, String> SOURCE_TYPE_LABELS = Map.of(
            "ACCOUNT", "收支流水",
            "PAYMENT_TRANSACTION", "付款交易",
            "BANK_STATEMENT", "银行流水",
            "GENERAL_LEDGER", "总账");

    /** 收支方向。 */
    private static final Map<String, String> TRANSACTION_TYPE_LABELS =
            Map.of("INCOME", "收入", "EXPENSE", "支出");

    /** 审批业务类型。 */
    private static final Map<String, String> BIZ_TYPE_LABELS = bizTypeLabels();

    /** 资产分类。 */
    private static final Map<String, String> ASSET_CATEGORY_LABELS = Map.of(
            "ELECTRONIC", "电子设备",
            "FURNITURE", "办公家具",
            "VEHICLE_TOOL", "车辆及工具",
            "OTHER", "其他");

    /** 资产状态。 */
    private static final Map<String, String> ASSET_STATUS_LABELS = Map.of(
            "IN_STOCK", "在库",
            "BORROWED", "借用中",
            "REPAIRING", "维修中",
            "SCRAPPED", "已报废");

    /** 财务报表。 */
    public void exportFinancial(FinancialReportVO report, OutputStream out) {
        List<List<Object>> rows = new ArrayList<>();
        if (report.getItems() != null) {
            for (FinancialReportItemVO item : report.getItems()) {
                rows.add(row(
                        formatDateTime(item.getTransactionDate()),
                        label(SOURCE_TYPE_LABELS, item.getSourceType()),
                        label(TRANSACTION_TYPE_LABELS, item.getType()),
                        item.getProjectName(),
                        item.getAccountName(),
                        item.getAmount(),
                        item.getDescription()));
            }
        }
        rows.add(row("合计收入", null, null, null, null, report.getTotalIncome(), null));
        rows.add(row("合计支出", null, null, null, null, report.getTotalExpense(), null));
        rows.add(row("净额", null, null, null, null, report.getNetAmount(), null));
        SimpleXlsxWriter.write(
                out,
                "财务报表",
                List.of("交易时间", "来源", "收支", "项目", "账户", "金额", "描述"),
                rows);
    }

    /** 项目成本报表。 */
    public void exportProjectCost(ProjectCostReportVO report, OutputStream out) {
        List<List<Object>> rows = new ArrayList<>();
        if (report.getItems() != null) {
            for (ProjectCostItemVO item : report.getItems()) {
                rows.add(row(
                        item.getProjectName(),
                        item.getContractAmount(),
                        item.getBudgetAmount(),
                        item.getChangedAmount(),
                        item.getPaidAmount(),
                        item.getAllocatedAmount(),
                        item.getActualAmount()));
            }
        }
        rows.add(row(
                "合计",
                null,
                report.getTotalBudget(),
                report.getTotalChanged(),
                report.getTotalPaid(),
                report.getTotalAllocated(),
                null));
        SimpleXlsxWriter.write(
                out,
                "项目成本",
                List.of("项目", "合同金额", "预算金额", "已变更", "已付进度款", "成本分摊", "实际已发生"),
                rows);
    }

    /** 材料报表。 */
    public void exportMaterial(MaterialReportVO report, OutputStream out) {
        List<List<Object>> rows = new ArrayList<>();
        if (report.getItems() != null) {
            for (MaterialReportItemVO item : report.getItems()) {
                rows.add(row(
                        item.getMaterialCode(),
                        item.getMaterialName(),
                        item.getSpecification(),
                        item.getUnit(),
                        item.getCurrentPrice(),
                        item.getTransactionCount(),
                        item.getTotalAmount()));
            }
        }
        rows.add(row("合计", null, null, null, null, report.getTotalCount(), report.getTotalAmount()));
        SimpleXlsxWriter.write(
                out,
                "材料报表",
                List.of("材料编码", "材料名", "规格", "单位", "当前单价", "领用/采购笔数", "金额合计"),
                rows);
    }

    /** 人力成本报表。 */
    public void exportHrCost(HrCostReportVO report, OutputStream out) {
        List<List<Object>> rows = new ArrayList<>();
        rows.add(row(
                "全员（" + text(report.getMonth()) + "）",
                report.getHeadcount(),
                report.getPayrollHeadcount(),
                report.getGrossPay(),
                report.getNetPay(),
                report.getSocialInsurance(),
                report.getHousingFund(),
                report.getTax(),
                report.getAvgNetPay()));
        if (report.getByDept() != null) {
            for (HrCostDeptVO dept : report.getByDept()) {
                rows.add(row(
                        dept.getDeptName(),
                        dept.getHeadcount(),
                        dept.getPayrollHeadcount(),
                        dept.getGrossPay(),
                        dept.getNetPay(),
                        dept.getSocialInsurance(),
                        dept.getHousingFund(),
                        dept.getTax(),
                        dept.getAvgNetPay()));
            }
        }
        SimpleXlsxWriter.write(
                out,
                "人力成本",
                List.of("部门", "在职人数", "工资人数", "应发合计", "实发合计", "社保", "公积金", "个税", "人均实发"),
                rows);
    }

    /** 绩效考核报表。 */
    public void exportHrPerformance(HrPerformanceReportVO report, OutputStream out) {
        List<List<Object>> rows = new ArrayList<>();
        rows.add(row(
                "全员（" + text(report.getPeriod()) + "）",
                report.getReviewCount(),
                report.getFinalizedCount(),
                report.getAvgFinalScore()));
        if (report.getByDept() != null) {
            for (HrPerformanceDeptVO dept : report.getByDept()) {
                rows.add(row(
                        dept.getDeptName(),
                        dept.getReviewCount(),
                        dept.getFinalizedCount(),
                        dept.getAvgFinalScore()));
            }
        }
        rows.add(row("分数分布 A（≥90）", report.getDistribution().getA(), null, null));
        rows.add(row("分数分布 B（80~89.99）", report.getDistribution().getB(), null, null));
        rows.add(row("分数分布 C（70~79.99）", report.getDistribution().getC(), null, null));
        rows.add(row("分数分布 D（<70）", report.getDistribution().getD(), null, null));
        SimpleXlsxWriter.write(
                out, "绩效考核", List.of("部门", "评审数", "定稿数", "平均分"), rows);
    }

    /** 行政资产报表。 */
    public void exportAdminAssets(AdminAssetReportVO report, OutputStream out) {
        List<List<Object>> rows = new ArrayList<>();
        rows.add(row("全部资产", report.getTotalCount(), report.getTotalValue()));
        if (report.getByCategory() != null) {
            for (AdminAssetCategoryStatVO category : report.getByCategory()) {
                rows.add(row(
                        label(ASSET_CATEGORY_LABELS, category.getCategory()),
                        category.getCount(),
                        category.getTotalValue()));
            }
        }
        if (report.getByStatus() != null) {
            report.getByStatus()
                    .forEach((status, count) ->
                            rows.add(row("状态：" + label(ASSET_STATUS_LABELS, status), count, null)));
        }
        rows.add(row("借用中", report.getBorrowingCount(), null));
        rows.add(row("逾期", report.getOverdueCount(), null));
        rows.add(row("借用率", report.getBorrowRate(), null));
        SimpleXlsxWriter.write(
                out, "行政资产", List.of("分类", "数量", "购置金额合计"), rows);
    }

    /** 审批效率报表。 */
    public void exportBpmEfficiency(BpmEfficiencyReportVO report, OutputStream out) {
        List<List<Object>> rows = new ArrayList<>();
        rows.add(row("汇总", "全部", report.getInstanceCount(), report.getApprovedCount(),
                report.getAvgDurationHours(), report.getOverdueTaskCount()));
        if (report.getByBizType() != null) {
            for (BpmBizTypeStatVO stat : report.getByBizType()) {
                rows.add(row(
                        "按业务类型",
                        label(BIZ_TYPE_LABELS, stat.getBizType()),
                        stat.getInstanceCount(),
                        stat.getApprovedCount(),
                        stat.getAvgDurationHours(),
                        null));
            }
        }
        if (report.getBacklogByNode() != null) {
            for (BpmNodeBacklogVO backlog : report.getBacklogByNode()) {
                rows.add(row("待办积压", backlog.getNodeKey(), null, null, null, backlog.getPendingCount()));
            }
        }
        SimpleXlsxWriter.write(
                out,
                "审批效率",
                List.of("统计维度", "业务类型/节点", "实例数", "通过数", "平均时长（h）", "待处理数"),
                rows);
    }

    // ------------------------------------------------------------------
    // 工具方法
    // ------------------------------------------------------------------

    private List<Object> row(Object... values) {
        List<Object> cells = new ArrayList<>(values.length);
        for (Object value : values) {
            cells.add(value);
        }
        return cells;
    }

    private String formatDateTime(LocalDateTime time) {
        return time == null ? "" : DATE_TIME.format(time);
    }

    private String label(Map<String, String> labels, String code) {
        if (code == null || code.isBlank()) {
            return "";
        }
        return labels.getOrDefault(code, code);
    }

    private String text(String value) {
        return value == null ? "" : value;
    }

    /** 审批业务类型中文名（与前端 bpmBizTypeLabels 保持一致）。 */
    private static Map<String, String> bizTypeLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("EXPENSE", "报销");
        labels.put("CHANGE_REQUEST", "项目变更");
        labels.put("HR_REGULAR", "HR 转正");
        labels.put("HR_TRANSFER", "HR 调岗");
        labels.put("HR_RESIGN", "HR 离职");
        labels.put("HR_LEAVE", "HR 请假");
        labels.put("HR_CHECKIN_SUPPLEMENT", "HR 补卡");
        labels.put("HR_SALARY_CHANGE", "HR 薪资变动");
        labels.put("HR_SALARY_CORRECTION", "HR 薪资更正");
        labels.put("HR_PERFORMANCE", "HR 绩效考评");
        labels.put("ADMIN_ASSET_BORROW", "行政资产领用");
        labels.put("ADMIN_MATERIAL_APPLY", "行政易耗品申领");
        labels.put("ADMIN_VEHICLE_APPLY", "行政用车申请");
        labels.put("ADMIN_MEETING_APPLY", "行政会议室预定");
        labels.put("ADMIN_REPAIR", "行政报修");
        return Map.copyOf(labels);
    }
}
