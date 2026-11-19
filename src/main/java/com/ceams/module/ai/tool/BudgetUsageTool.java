package com.ceams.module.ai.tool;

import com.ceams.common.exception.BusinessException;
import com.ceams.module.ai.config.AiProperties;
import com.ceams.module.project.service.BudgetService;
import com.ceams.module.project.service.ProjectService;
import com.ceams.module.project.vo.BudgetVO;
import com.ceams.module.project.vo.ProjectVO;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** 项目预算使用率只读工具。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BudgetUsageTool implements ReadOnlyTool {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final BudgetService budgetService;
    private final ProjectService projectService;
    private final AiProperties aiProperties;

    @Override
    public String name() {
        return "queryBudgetUsage";
    }

    @Override
    public String description() {
        return "查询项目预算使用情况：预算金额、已用金额、剩余金额、使用率与超支标记。"
                + "projectId 可选，用于限定单个项目。"
                + "STAFF 角色只能看到本人担任项目经理的项目预算。";
    }

    @Override
    public Map<String, Object> parameterSchema() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("projectId", ToolSchemas.text("项目 ID，可传数字或数字字符串，可选"));
        properties.put("limit", ToolSchemas.integer("返回行数，默认 20，最大 200", 1, 200));
        return ToolSchemas.object(properties);
    }

    @Override
    public ToolResult execute(Map<String, Object> args) {
        int limit = ToolArgs.limit(args, aiProperties.getMaxToolRows());
        Long projectId = ToolArgs.longValue(args, "projectId");
        Long userId = ToolSecurity.currentUserId();
        boolean unrestricted = ToolSecurity.isFinanceOrAdmin();
        if (!unrestricted && userId == null) {
            return ToolResult.denied("无法识别当前用户，已拒绝查询预算数据");
        }
        int pageSize = Math.min(limit, PageCollector.MAX_PAGE_SIZE);
        PageCollector.Result<BudgetVO> collected =
                PageCollector.collect(
                        page ->
                                unrestricted
                                        ? budgetService.page(page, pageSize, projectId)
                                        : budgetService.pageForManager(
                                                page, pageSize, projectId, userId),
                        limit);
        Map<Long, String> projectNames = new HashMap<>();
        List<Map<String, Object>> rows = new ArrayList<>();
        for (BudgetVO vo : collected.items()) {
            rows.add(toRow(vo, projectNames));
        }
        return ToolResult.capped(
                rows,
                (int) Math.min(collected.total(), Integer.MAX_VALUE),
                aiProperties.getMaxToolRows());
    }

    private Map<String, Object> toRow(BudgetVO vo, Map<Long, String> projectNames) {
        BigDecimal amount = amount(vo.getAmount());
        BigDecimal used = amount(vo.getUsedAmount());
        BigDecimal remaining =
                vo.getRemainingAmount() == null ? amount.subtract(used) : vo.getRemainingAmount();
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("budgetId", vo.getId() == null ? null : String.valueOf(vo.getId()));
        row.put("budgetName", vo.getBudgetName());
        row.put("projectId", vo.getProjectId() == null ? null : String.valueOf(vo.getProjectId()));
        row.put("projectName", projectName(vo.getProjectId(), projectNames));
        row.put("amount", amount);
        row.put("usedAmount", used);
        row.put("remainingAmount", remaining);
        row.put("usageRate", usageRate(amount, used));
        row.put("overBudget", amount.compareTo(BigDecimal.ZERO) > 0 && used.compareTo(amount) > 0);
        row.put("status", vo.getStatus() == null ? null : vo.getStatus().name());
        row.put("startDate", vo.getStartDate());
        row.put("endDate", vo.getEndDate());
        return row;
    }

    private String projectName(Long projectId, Map<Long, String> cache) {
        if (projectId == null) {
            return null;
        }
        return cache.computeIfAbsent(projectId, this::loadProjectName);
    }

    private String loadProjectName(Long projectId) {
        try {
            ProjectVO project = projectService.detail(projectId);
            return project == null ? null : project.getProjectName();
        } catch (BusinessException ex) {
            log.debug("预算关联项目 {} 不存在，忽略项目名", projectId);
            return null;
        }
    }

    private static BigDecimal usageRate(BigDecimal amount, BigDecimal used) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return used.multiply(HUNDRED).divide(amount, 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal amount(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
