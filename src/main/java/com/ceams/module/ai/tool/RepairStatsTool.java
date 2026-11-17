package com.ceams.module.ai.tool;

import com.ceams.module.admin.service.AdminRepairService;
import com.ceams.module.admin.vo.RepairStatsVO;
import com.ceams.module.ai.config.AiProperties;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 报修工单统计只读工具。
 *
 * <p>数据范围：SYS_ADMIN 全量；STAFF 仅本人提交（按报修人 sys_user.id 过滤）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RepairStatsTool implements ReadOnlyTool {

    /** 日期参数格式：yyyy-MM-dd。 */
    private static final Pattern DATE_PATTERN = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");

    private static final String INVALID_DATE = "参数格式不正确：%s 必须为 yyyy-MM-dd";

    private final AdminRepairService adminRepairService;

    private final AiProperties aiProperties;

    @Override
    public String name() {
        return "queryRepairStats";
    }

    @Override
    public String description() {
        return "查询某日期区间的报修工单统计：工单总数、待受理/处理中/已完成数量、完成率、平均处理时长。"
                + "start、end 必填，格式 yyyy-MM-dd；category 可选（报修分类编码）。"
                + "SYS_ADMIN 可查全量；STAFF 只能查看本人提交的工单统计。";
    }

    @Override
    public Map<String, Object> parameterSchema() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("start", ToolSchemas.text("开始日期，格式 yyyy-MM-dd，必填"));
        properties.put("end", ToolSchemas.text("结束日期，格式 yyyy-MM-dd，必填"));
        properties.put("category", ToolSchemas.text("报修分类编码，可选"));
        return ToolSchemas.object(properties, "start", "end");
    }

    @Override
    public ToolResult execute(Map<String, Object> args) {
        boolean admin = ToolSecurity.isAdmin();
        boolean staff = ToolSecurity.hasAnyRole("STAFF");
        if (!admin && !staff) {
            return ToolResult.denied("无权查询该数据");
        }
        LocalDate start = parseDate(args, "start");
        if (start == null) {
            return ToolResult.denied(String.format(INVALID_DATE, "start"));
        }
        LocalDate end = parseDate(args, "end");
        if (end == null) {
            return ToolResult.denied(String.format(INVALID_DATE, "end"));
        }
        if (start.isAfter(end)) {
            return ToolResult.denied("参数格式不正确：start 不能晚于 end");
        }

        // stats 第 4 参语义为报修人 sys_user.id，因此 STAFF 直接用当前登录用户 ID 过滤
        Long employeeUserId = null;
        if (!admin) {
            employeeUserId = ToolSecurity.currentUserId();
            if (employeeUserId == null) {
                return ToolResult.denied("无法识别当前用户，已拒绝查询");
            }
        }
        String category = ToolArgs.text(args, "category");

        RepairStatsVO stats = adminRepairService.stats(start, end, category, employeeUserId);
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("start", start.toString());
        row.put("end", end.toString());
        row.put("category", category);
        row.put("scope", admin ? "ALL" : "SELF");
        if (stats != null) {
            row.put("totalCount", stats.getTotalCount());
            row.put("submittedCount", stats.getSubmittedCount());
            row.put("processingCount", stats.getProcessingCount());
            row.put("completedCount", stats.getCompletedCount());
            row.put("completionRate", plain(stats.getCompletionRate()));
            row.put("avgHandleHours", plain(stats.getAvgHandleHours()));
        }
        return ToolResult.capped(List.of(row), 1, aiProperties.getMaxToolRows());
    }

    /** 读取并校验 yyyy-MM-dd 日期；未传或格式非法返回 null。 */
    private static LocalDate parseDate(Map<String, Object> args, String key) {
        String text = ToolArgs.text(args, key);
        if (text == null || !DATE_PATTERN.matcher(text).matches()) {
            return null;
        }
        try {
            return LocalDate.parse(text);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /** 比率/时长转明文字符串，避免科学计数法。 */
    private static String plain(BigDecimal value) {
        return value == null ? null : value.toPlainString();
    }
}
