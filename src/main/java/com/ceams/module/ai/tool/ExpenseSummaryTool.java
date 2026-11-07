package com.ceams.module.ai.tool;

import com.ceams.module.expense.service.ReceiptService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** 期间报销汇总只读工具。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExpenseSummaryTool implements ReadOnlyTool {

    private final ReceiptService receiptService;

    @Override
    public String name() {
        return "queryExpenseSummary";
    }

    @Override
    public String description() {
        return "查询某期间的报销汇总：报销合计金额、单据数、审批/付款/核验状态分布与失败类型分布。"
                + "period 支持 yyyy-MM 或 yyyy-MM-dd~yyyy-MM-dd，未传默认当前月；"
                + "keyword 可匹配票据编号或说明。STAFF 角色只能看到本人上报的报销。";
    }

    @Override
    public Map<String, Object> parameterSchema() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("period", ToolSchemas.period());
        properties.put("keyword", ToolSchemas.text("票据编号或说明关键词，可选"));
        return ToolSchemas.object(properties);
    }

    @Override
    public ToolResult execute(Map<String, Object> args) {
        ToolPeriod period = ToolPeriod.resolve(args);
        String keyword = ToolArgs.text(args, "keyword");
        Long userId = ToolSecurity.currentUserId();
        boolean unrestricted = ToolSecurity.isFinanceOrAdmin();
        if (!unrestricted && userId == null) {
            return ToolResult.denied("无法识别当前用户，已拒绝查询报销数据");
        }
        ReceiptService.ExpenseSummary summary =
                receiptService.summary(
                        period.start(), period.end(), unrestricted ? null : userId, keyword);

        Map<String, Object> row = new LinkedHashMap<>();
        row.put("period", period.label());
        if (period.fallback()) {
            row.put("periodFallback", true);
        }
        row.put("totalAmount", summary.totalAmount());
        row.put("receiptCount", summary.totalCount());
        row.put("pendingCount", summary.approvalStatusDistribution().getOrDefault("PENDING", 0L));
        row.put("approvalStatusDistribution", summary.approvalStatusDistribution());
        row.put("paymentStatusDistribution", summary.paymentStatusDistribution());
        row.put("verificationStatusDistribution", summary.verificationStatusDistribution());
        row.put("typeDistribution", summary.typeDistribution());
        return new ToolResult(List.of(row), 1, false);
    }
}
