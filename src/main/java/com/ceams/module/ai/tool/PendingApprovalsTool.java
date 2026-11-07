package com.ceams.module.ai.tool;

import com.ceams.module.ai.config.AiProperties;
import com.ceams.module.expense.service.ReceiptService;
import com.ceams.module.expense.vo.ReceiptVO;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** 待审批报销清单只读工具。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PendingApprovalsTool implements ReadOnlyTool {

    private final ReceiptService receiptService;
    private final AiProperties aiProperties;

    @Override
    public String name() {
        return "queryPendingApprovals";
    }

    @Override
    public String description() {
        return "查询当前待审批的报销单据清单与金额合计，按创建时间倒序。"
                + "keyword 可匹配票据编号或说明；projectId 可选。"
                + "STAFF 角色只返回本人上报的单据，FINANCE / SYS_ADMIN 返回全部待审批单据。";
    }

    @Override
    public Map<String, Object> parameterSchema() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("keyword", ToolSchemas.text("票据编号或说明关键词，可选"));
        properties.put("projectId", ToolSchemas.text("项目 ID，可传数字或数字字符串，可选"));
        properties.put("limit", ToolSchemas.integer("返回行数，默认 20，最大 200", 1, 200));
        return ToolSchemas.object(properties);
    }

    @Override
    public ToolResult execute(Map<String, Object> args) {
        int limit = ToolArgs.limit(args, aiProperties.getMaxToolRows());
        String keyword = ToolArgs.text(args, "keyword");
        Long projectId = ToolArgs.longValue(args, "projectId");
        Long userId = ToolSecurity.currentUserId();
        boolean unrestricted = ToolSecurity.isFinanceOrAdmin();
        if (!unrestricted && userId == null) {
            return ToolResult.denied("无法识别当前用户，已拒绝查询待审批数据");
        }
        int itemLimit = Math.max(1, limit - 1);
        ReceiptService.PendingApprovalPage page =
                receiptService.pendingApprovals(
                        unrestricted ? null : userId, keyword, projectId, itemLimit);

        List<Map<String, Object>> rows = new ArrayList<>();
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("summary", true);
        summary.put("pendingCount", page.pendingCount());
        summary.put("pendingAmount", page.pendingAmount());
        rows.add(summary);
        for (ReceiptVO receipt : page.items()) {
            rows.add(toRow(receipt));
        }
        boolean truncated = page.pendingCount() > page.items().size();
        return ToolResult.capped(
                rows,
                (int) Math.min(page.pendingCount(), Integer.MAX_VALUE),
                truncated,
                aiProperties.getMaxToolRows());
    }

    private static Map<String, Object> toRow(ReceiptVO vo) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("receiptId", vo.getId() == null ? null : String.valueOf(vo.getId()));
        row.put("receiptNumber", vo.getReceiptNumber());
        row.put("amount", vo.getAmount());
        row.put(
                "approvalStatus",
                vo.getApprovalStatus() == null ? null : vo.getApprovalStatus().name());
        row.put("currentApprovalLevel", vo.getCurrentApprovalLevel());
        row.put("projectId", vo.getProjectId() == null ? null : String.valueOf(vo.getProjectId()));
        row.put("userId", vo.getUserId() == null ? null : String.valueOf(vo.getUserId()));
        row.put("receiptDate", vo.getReceiptDate());
        row.put("description", vo.getDescription());
        return row;
    }
}
