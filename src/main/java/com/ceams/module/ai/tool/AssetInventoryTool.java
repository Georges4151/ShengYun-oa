package com.ceams.module.ai.tool;

import com.ceams.module.admin.service.AdminAssetService;
import com.ceams.module.admin.vo.AssetInventorySummaryVO;
import com.ceams.module.ai.config.AiProperties;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** 行政资产盘点汇总只读工具（仅 SYS_ADMIN）。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AssetInventoryTool implements ReadOnlyTool {

    private final AdminAssetService adminAssetService;

    private final AiProperties aiProperties;

    @Override
    public String name() {
        return "queryAssetInventory";
    }

    @Override
    public String description() {
        return "查询行政资产盘点汇总：资产总数、在库/借出/维修中/已报废数量、购置金额合计、"
                + "逾期未归还数量。category 可选（资产分类编码）；仅 SYS_ADMIN 角色可调用。";
    }

    @Override
    public Map<String, Object> parameterSchema() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("category", ToolSchemas.text("资产分类编码，可选"));
        return ToolSchemas.object(properties);
    }

    @Override
    public ToolResult execute(Map<String, Object> args) {
        if (!ToolSecurity.isAdmin()) {
            return ToolResult.denied("无权查询该数据");
        }
        String category = ToolArgs.text(args, "category");
        AssetInventorySummaryVO summary = adminAssetService.inventorySummary(category);
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("category", category);
        if (summary != null) {
            row.put("totalCount", summary.getTotalCount());
            row.put("inStockCount", summary.getInStockCount());
            row.put("borrowedCount", summary.getBorrowedCount());
            row.put("repairingCount", summary.getRepairingCount());
            row.put("scrappedCount", summary.getScrappedCount());
            row.put("totalValue", plain(summary.getTotalValue()));
            row.put("overdueCount", summary.getOverdueCount());
        }
        return ToolResult.capped(List.of(row), 1, aiProperties.getMaxToolRows());
    }

    /** 金额转明文字符串，避免科学计数法。 */
    private static String plain(BigDecimal value) {
        return value == null ? null : value.toPlainString();
    }
}
