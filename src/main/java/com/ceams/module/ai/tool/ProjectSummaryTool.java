package com.ceams.module.ai.tool;

import com.ceams.module.ai.config.AiProperties;
import com.ceams.module.project.enums.ProjectStatus;
import com.ceams.module.project.service.ProjectService;
import com.ceams.module.project.vo.ProjectVO;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** 项目台账只读工具。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProjectSummaryTool implements ReadOnlyTool {

    private final ProjectService projectService;
    private final AiProperties aiProperties;

    @Override
    public String name() {
        return "queryProjectSummary";
    }

    @Override
    public String description() {
        return "按关键词或状态查询项目台账，返回合同额、预算额、实际发生额、项目状态与项目经理。"
                + "keyword 可匹配项目名或客户名；status 可选 PLANNING/IN_PROGRESS/PAUSED/"
                + "COMPLETED/CLOSED。STAFF 角色只能看到本人担任项目经理的项目。";
    }

    @Override
    public Map<String, Object> parameterSchema() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("keyword", ToolSchemas.text("项目名或客户名关键词，可选"));
        properties.put(
                "status",
                ToolSchemas.choice(
                        "项目状态，可选", "PLANNING", "IN_PROGRESS", "PAUSED", "COMPLETED", "CLOSED"));
        properties.put("limit", ToolSchemas.integer("返回行数，默认 20，最大 200", 1, 200));
        return ToolSchemas.object(properties);
    }

    @Override
    public ToolResult execute(Map<String, Object> args) {
        int limit = ToolArgs.limit(args, aiProperties.getMaxToolRows());
        String keyword = ToolArgs.text(args, "keyword");
        ProjectStatus status = ToolArgs.enumValue(args, "status", ProjectStatus.class);
        Long userId = ToolSecurity.currentUserId();
        boolean unrestricted = ToolSecurity.isFinanceOrAdmin();
        if (!unrestricted && userId == null) {
            return ToolResult.denied("无法识别当前用户，已拒绝查询项目数据");
        }
        int pageSize = Math.min(limit, PageCollector.MAX_PAGE_SIZE);
        PageCollector.Result<ProjectVO> collected =
                PageCollector.collect(
                        page ->
                                unrestricted
                                        ? projectService.page(page, pageSize, keyword, status)
                                        : projectService.pageForManager(
                                                page, pageSize, keyword, status, userId),
                        limit);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (ProjectVO vo : collected.items()) {
            rows.add(toRow(vo));
        }
        return ToolResult.capped(
                rows,
                (int) Math.min(collected.total(), Integer.MAX_VALUE),
                aiProperties.getMaxToolRows());
    }

    private static Map<String, Object> toRow(ProjectVO vo) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("projectId", id(vo.getId()));
        row.put("projectName", vo.getProjectName());
        row.put("clientName", vo.getClientName());
        row.put("contractAmount", amount(vo.getContractAmount()));
        row.put("budgetAmount", amount(vo.getBudgetAmount()));
        row.put("actualAmount", amount(vo.getActualAmount()));
        row.put("status", vo.getStatus() == null ? null : vo.getStatus().name());
        row.put("managerName", vo.getManagerName());
        row.put("startDate", vo.getStartDate());
        row.put("endDate", vo.getEndDate());
        return row;
    }

    private static String id(Long value) {
        return value == null ? null : String.valueOf(value);
    }

    private static BigDecimal amount(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
