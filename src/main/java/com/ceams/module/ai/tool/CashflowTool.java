package com.ceams.module.ai.tool;

import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.module.ai.config.AiProperties;
import com.ceams.module.report.dto.FinancialReportQuery;
import com.ceams.module.report.service.ReportService;
import com.ceams.module.report.vo.FinancialReportItemVO;
import com.ceams.module.report.vo.FinancialReportVO;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** 期间资金收支只读工具（仅 FINANCE / SYS_ADMIN）。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CashflowTool implements ReadOnlyTool {

    private static final String SOURCE_ACCOUNT = "ACCOUNT";

    private static final String SOURCE_PAYMENT_TRANSACTION = "PAYMENT_TRANSACTION";

    private static final String TYPE_INCOME = "INCOME";

    private static final String TYPE_EXPENSE = "EXPENSE";

    private static final String UNCATEGORIZED = "未分类";

    /** 单次查询最多跨月数，避免模型传入超长区间拖慢数据库。 */
    private static final int MAX_MONTHS = 24;

    /** 收入/支出各返回的 Top 分类数上限。 */
    private static final int DEFAULT_TOP_N = 10;

    private final ReportService reportService;
    private final AiProperties aiProperties;

    @Override
    public String name() {
        return "queryCashflow";
    }

    @Override
    public String description() {
        return "查询某期间的资金收支：收入合计、支出合计、净额以及收入/支出 Top 分类。"
                + "period 支持 yyyy-MM 或 yyyy-MM-dd~yyyy-MM-dd，未传默认当前月；"
                + "projectId 可选。仅 FINANCE / SYS_ADMIN 角色可调用。";
    }

    @Override
    public Map<String, Object> parameterSchema() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("period", ToolSchemas.period());
        properties.put("projectId", ToolSchemas.text("项目 ID，可传数字或数字字符串，可选"));
        properties.put("limit", ToolSchemas.integer("Top 分类数量，默认 10，最大 200", 1, 200));
        return ToolSchemas.object(properties);
    }

    @Override
    public ToolResult execute(Map<String, Object> args) {
        if (!ToolSecurity.isFinanceOrAdmin()) {
            return ToolResult.denied("当前角色无权查看该数据");
        }
        ToolPeriod period = ToolPeriod.resolve(args);
        Long projectId = ToolArgs.longValue(args, "projectId");
        int limit = ToolArgs.limit(args, aiProperties.getMaxToolRows());
        ensureMonthSpan(period);

        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalExpense = BigDecimal.ZERO;
        Map<String, BigDecimal> incomeByCategory = new LinkedHashMap<>();
        Map<String, BigDecimal> expenseByCategory = new LinkedHashMap<>();

        for (YearMonth month = period.firstMonth();
                !month.isAfter(period.lastMonth());
                month = month.plusMonths(1)) {
            FinancialReportQuery query = new FinancialReportQuery();
            query.setMonth(month.toString());
            query.setProjectId(projectId);
            FinancialReportVO report = reportService.financial(query);
            List<FinancialReportItemVO> items = report == null ? List.of() : report.getItems();
            if (items == null) {
                continue;
            }
            for (FinancialReportItemVO item : items) {
                if (!isCashflowSource(item.getSourceType())
                        || !period.contains(item.getTransactionDate())) {
                    continue;
                }
                BigDecimal amount = item.getAmount() == null ? BigDecimal.ZERO : item.getAmount();
                String category = category(item.getDescription());
                if (TYPE_INCOME.equals(item.getType())) {
                    totalIncome = totalIncome.add(amount);
                    incomeByCategory.merge(category, amount, BigDecimal::add);
                } else if (TYPE_EXPENSE.equals(item.getType())) {
                    totalExpense = totalExpense.add(amount);
                    expenseByCategory.merge(category, amount, BigDecimal::add);
                }
            }
        }

        int topN = Math.min(limit, DEFAULT_TOP_N);
        List<Map<String, Object>> rows = new ArrayList<>();
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("summary", true);
        summary.put("period", period.label());
        if (period.fallback()) {
            summary.put("periodFallback", true);
        }
        summary.put("totalIncome", totalIncome);
        summary.put("totalExpense", totalExpense);
        summary.put("netAmount", totalIncome.subtract(totalExpense));
        rows.add(summary);
        appendTop(rows, TYPE_EXPENSE, expenseByCategory, topN);
        appendTop(rows, TYPE_INCOME, incomeByCategory, topN);
        boolean truncated = expenseByCategory.size() > topN || incomeByCategory.size() > topN;
        return ToolResult.capped(rows, rows.size(), truncated, aiProperties.getMaxToolRows());
    }

    private static void ensureMonthSpan(ToolPeriod period) {
        long months = ChronoUnit.MONTHS.between(period.firstMonth(), period.lastMonth()) + 1;
        if (months > MAX_MONTHS) {
            throw BusinessException.of(
                    ErrorCode.VALID_ILLEGAL_ARGUMENT, "资金收支查询区间不能超过 " + MAX_MONTHS + " 个月");
        }
    }

    private static boolean isCashflowSource(String sourceType) {
        return SOURCE_ACCOUNT.equals(sourceType) || SOURCE_PAYMENT_TRANSACTION.equals(sourceType);
    }

    private static String category(String description) {
        return description == null || description.isBlank() ? UNCATEGORIZED : description.trim();
    }

    private static void appendTop(
            List<Map<String, Object>> rows,
            String type,
            Map<String, BigDecimal> categories,
            int topN) {
        categories.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .limit(topN)
                .forEach(
                        entry -> {
                            Map<String, Object> row = new LinkedHashMap<>();
                            row.put("category", entry.getKey());
                            row.put("type", type);
                            row.put("amount", entry.getValue());
                            rows.add(row);
                        });
    }
}
