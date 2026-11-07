package com.ceams.module.ai.tool;

import com.ceams.module.ai.config.AiProperties;
import com.ceams.module.seal.service.ElectronicSealUsageService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** 期间用印统计只读工具（仅 FINANCE / SYS_ADMIN）。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SealUsageTool implements ReadOnlyTool {

    private final ElectronicSealUsageService electronicSealUsageService;
    private final AiProperties aiProperties;

    @Override
    public String name() {
        return "querySealUsage";
    }

    @Override
    public String description() {
        return "查询某期间的电子印章使用次数与用印人分布，按使用次数倒序。"
                + "period 支持 yyyy-MM 或 yyyy-MM-dd~yyyy-MM-dd，未传默认当前月；"
                + "sealId 与 userId 可选。仅 FINANCE / SYS_ADMIN 角色可调用。";
    }

    @Override
    public Map<String, Object> parameterSchema() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("period", ToolSchemas.period());
        properties.put("sealId", ToolSchemas.text("印章 ID，可传数字或数字字符串，可选"));
        properties.put("userId", ToolSchemas.text("用印人用户 ID，可传数字或数字字符串，可选"));
        properties.put("limit", ToolSchemas.integer("返回用印人分布行数，默认 20，最大 200", 1, 200));
        return ToolSchemas.object(properties);
    }

    @Override
    public ToolResult execute(Map<String, Object> args) {
        if (!ToolSecurity.isFinanceOrAdmin()) {
            return ToolResult.denied("当前角色无权查看该数据");
        }
        ToolPeriod period = ToolPeriod.resolve(args);
        Long sealId = ToolArgs.longValue(args, "sealId");
        Long userId = ToolArgs.longValue(args, "userId");
        int limit = ToolArgs.limit(args, aiProperties.getMaxToolRows());
        ElectronicSealUsageService.UsageSummary summary =
                electronicSealUsageService.summary(period.start(), period.end(), sealId, userId);

        int topN = Math.max(1, limit - 1);
        List<Map<String, Object>> rows = new ArrayList<>();
        Map<String, Object> summaryRow = new LinkedHashMap<>();
        summaryRow.put("summary", true);
        summaryRow.put("period", period.label());
        if (period.fallback()) {
            summaryRow.put("periodFallback", true);
        }
        summaryRow.put("usageCount", summary.totalCount());
        rows.add(summaryRow);

        List<Map.Entry<String, Long>> distribution =
                summary.userDistribution().entrySet().stream()
                        .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                        .toList();
        distribution.stream()
                .limit(topN)
                .forEach(
                        entry -> {
                            Map<String, Object> row = new LinkedHashMap<>();
                            row.put("userName", entry.getKey());
                            row.put("usageCount", entry.getValue());
                            rows.add(row);
                        });
        boolean truncated = distribution.size() > topN;
        return ToolResult.capped(
                rows,
                (int) Math.min(summary.totalCount(), Integer.MAX_VALUE),
                truncated,
                aiProperties.getMaxToolRows());
    }
}
