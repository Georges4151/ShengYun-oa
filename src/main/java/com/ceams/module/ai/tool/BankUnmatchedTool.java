package com.ceams.module.ai.tool;

import com.ceams.module.ai.config.AiProperties;
import com.ceams.module.bank.enums.BankStatementStatus;
import com.ceams.module.bank.service.BankStatementService;
import com.ceams.module.bank.vo.BankStatementVO;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** 期间未对账银行流水只读工具（仅 FINANCE / SYS_ADMIN）。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BankUnmatchedTool implements ReadOnlyTool {

    private final BankStatementService bankStatementService;
    private final AiProperties aiProperties;

    @Override
    public String name() {
        return "queryBankUnmatched";
    }

    @Override
    public String description() {
        return "查询某期间未对账的银行流水笔数与金额合计，并返回明细（交易日期、金额、收支方向、摘要、对方户名）。"
                + "period 支持 yyyy-MM 或 yyyy-MM-dd~yyyy-MM-dd，未传默认当前月；accountId 可选。"
                + "仅 FINANCE / SYS_ADMIN 角色可调用。";
    }

    @Override
    public Map<String, Object> parameterSchema() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("period", ToolSchemas.period());
        properties.put("accountId", ToolSchemas.text("付款账户 ID，可传数字或数字字符串，可选"));
        properties.put("limit", ToolSchemas.integer("返回明细行数，默认 20，最大 200", 1, 200));
        return ToolSchemas.object(properties);
    }

    @Override
    public ToolResult execute(Map<String, Object> args) {
        if (!ToolSecurity.isFinanceOrAdmin()) {
            return ToolResult.denied("当前角色无权查看该数据");
        }
        ToolPeriod period = ToolPeriod.resolve(args);
        Long accountId = ToolArgs.longValue(args, "accountId");
        int limit = ToolArgs.limit(args, aiProperties.getMaxToolRows());
        int itemLimit = Math.max(1, limit - 1);
        BankStatementService.UnmatchedSummary summary =
                bankStatementService.unmatchedSummary(period.start(), period.end(), accountId);

        int pageSize = Math.min(itemLimit, PageCollector.MAX_PAGE_SIZE);
        PageCollector.Result<BankStatementVO> collected =
                PageCollector.collect(
                        page ->
                                bankStatementService.page(
                                        page,
                                        pageSize,
                                        accountId,
                                        BankStatementStatus.UNRECONCILED,
                                        null,
                                        period.start(),
                                        period.end()),
                        itemLimit);

        List<Map<String, Object>> rows = new ArrayList<>();
        Map<String, Object> summaryRow = new LinkedHashMap<>();
        summaryRow.put("summary", true);
        summaryRow.put("period", period.label());
        if (period.fallback()) {
            summaryRow.put("periodFallback", true);
        }
        summaryRow.put("unmatchedCount", summary.unmatchedCount());
        summaryRow.put("unmatchedAmount", summary.totalAmount());
        rows.add(summaryRow);
        for (BankStatementVO statement : collected.items()) {
            rows.add(toRow(statement));
        }
        boolean truncated =
                summary.unmatchedCount() > collected.items().size() || collected.truncated();
        return ToolResult.capped(
                rows,
                (int) Math.min(summary.unmatchedCount(), Integer.MAX_VALUE),
                truncated,
                aiProperties.getMaxToolRows());
    }

    private static Map<String, Object> toRow(BankStatementVO vo) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("statementId", vo.getId() == null ? null : String.valueOf(vo.getId()));
        row.put("transactionDate", vo.getTransactionDate());
        row.put("amount", vo.getAmount());
        row.put("type", vo.getType() == null ? null : vo.getType().name());
        row.put("description", vo.getDescription());
        row.put("counterparty", vo.getCounterparty());
        row.put("accountId", vo.getAccountId() == null ? null : String.valueOf(vo.getAccountId()));
        row.put("status", vo.getStatus() == null ? null : vo.getStatus().name());
        return row;
    }
}
