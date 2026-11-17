package com.ceams.module.ai.tool;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 只读工具注册表。
 *
 * <p>启动时强制校验工具名：非空、唯一且必须命中 15 个白名单之一。任何未知工具都会让应用启动失败， 这是防止模型触达任意查询的安全红线。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ToolRegistry {

    /** 方案 §6 定义的 15 个只读工具白名单。 */
    public static final Set<String> ALLOWED_TOOL_NAMES =
            Set.of(
                    "queryProjectSummary",
                    "queryReceivable",
                    "queryExpenseSummary",
                    "queryCashflow",
                    "queryPendingApprovals",
                    "queryBankUnmatched",
                    "queryBankMatchCandidates",
                    "queryBudgetUsage",
                    "querySealUsage",
                    "queryHrHeadcount",
                    "queryAttendanceSummary",
                    "queryPayrollSummary",
                    "queryAssetInventory",
                    "queryVehicleUsage",
                    "queryRepairStats");

    private final List<ReadOnlyTool> tools;

    private List<ReadOnlyTool> orderedTools = List.of();

    /** 启动校验：工具名非空、唯一且必须在白名单内。 */
    @PostConstruct
    public void validate() {
        Set<String> seen = new LinkedHashSet<>();
        for (ReadOnlyTool tool : tools) {
            String name = tool.name();
            if (name == null || name.isBlank()) {
                throw new IllegalStateException("只读工具名不能为空");
            }
            if (!ALLOWED_TOOL_NAMES.contains(name)) {
                throw new IllegalStateException("禁止注册未授权的只读工具：" + name);
            }
            if (!seen.add(name)) {
                throw new IllegalStateException("只读工具名重复：" + name);
            }
        }
        this.orderedTools =
                tools.stream().sorted(Comparator.comparing(ReadOnlyTool::name)).toList();
        log.info("只读工具注册表校验通过，共 {} 个：{}", orderedTools.size(), seen);
    }

    /** 按名称查找工具。 */
    public Optional<ReadOnlyTool> find(String name) {
        return orderedTools.stream().filter(tool -> tool.name().equals(name)).findFirst();
    }

    /** 全部已注册工具。 */
    public List<ReadOnlyTool> all() {
        return List.copyOf(orderedTools);
    }

    /** 生成 Ollama {@code /api/chat} 的 {@code tools} 数组。 */
    public List<Map<String, Object>> definitions() {
        List<Map<String, Object>> definitions = new ArrayList<>();
        for (ReadOnlyTool tool : orderedTools) {
            Map<String, Object> function = new LinkedHashMap<>();
            function.put("name", tool.name());
            function.put("description", tool.description());
            function.put("parameters", tool.parameterSchema());

            Map<String, Object> definition = new LinkedHashMap<>();
            definition.put("type", "function");
            definition.put("function", function);
            definitions.add(definition);
        }
        return definitions;
    }
}
