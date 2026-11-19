package com.ceams.module.ai.tool;

import com.ceams.common.exception.BusinessException;
import com.ceams.module.ai.config.AiProperties;
import com.ceams.module.hr.service.HrCheckinService;
import com.ceams.module.hr.service.HrEmployeeService;
import com.ceams.module.hr.vo.AttendanceSummaryVO;
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
 * 考勤汇总只读工具。
 *
 * <p>数据范围：SYS_ADMIN 可查全量并可传 deptId；STAFF 强制只查本人（员工档案缺失则拒绝）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AttendanceSummaryTool implements ReadOnlyTool {

    /** 日期参数格式：yyyy-MM-dd。 */
    private static final Pattern DATE_PATTERN = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");

    private static final String INVALID_DATE = "参数格式不正确：%s 必须为 yyyy-MM-dd";

    private final HrCheckinService hrCheckinService;

    private final HrEmployeeService hrEmployeeService;

    private final AiProperties aiProperties;

    @Override
    public String name() {
        return "queryAttendanceSummary";
    }

    @Override
    public String description() {
        return "查询某一日期区间的考勤汇总：打卡员工数、正常/迟到/早退/缺卡次数、补卡条数。"
                + "start、end 必填，格式 yyyy-MM-dd。SYS_ADMIN 可查全量（deptId 可选）；"
                + "STAFF 只能查看本人考勤汇总。";
    }

    @Override
    public Map<String, Object> parameterSchema() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("start", ToolSchemas.text("开始日期，格式 yyyy-MM-dd，必填"));
        properties.put("end", ToolSchemas.text("结束日期，格式 yyyy-MM-dd，必填"));
        properties.put("deptId", ToolSchemas.text("部门 ID，可传数字或数字字符串，可选（仅 SYS_ADMIN 生效）"));
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

        Long deptId = admin ? ToolArgs.longValue(args, "deptId") : null;
        Long employeeId = null;
        if (!admin) {
            try {
                employeeId = hrEmployeeService.currentEmployeeId();
            } catch (BusinessException e) {
                log.debug("STAFF 查询考勤汇总时未找到员工档案：{}", e.getMessage());
                return ToolResult.denied("无法识别当前员工档案，已拒绝查询");
            }
            if (employeeId == null) {
                return ToolResult.denied("无法识别当前员工档案，已拒绝查询");
            }
        }

        AttendanceSummaryVO summary = hrCheckinService.summary(start, end, deptId, employeeId);
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("start", start.toString());
        row.put("end", end.toString());
        row.put("scope", admin ? "ALL" : "SELF");
        row.put("deptId", deptId == null ? null : String.valueOf(deptId));
        row.put("employeeId", employeeId == null ? null : String.valueOf(employeeId));
        if (summary != null) {
            row.put("employeeCount", summary.getEmployeeCount());
            row.put("normalCount", summary.getNormalCount());
            row.put("lateCount", summary.getLateCount());
            row.put("earlyCount", summary.getEarlyCount());
            row.put("absentCount", summary.getAbsentCount());
            row.put("supplementCount", summary.getSupplementCount());
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
}
