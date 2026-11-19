package com.ceams.module.ai.tool;

import com.ceams.module.admin.service.AdminVehicleService;
import com.ceams.module.admin.vo.VehicleUsageItemVO;
import com.ceams.module.admin.vo.VehicleUsageSummaryVO;
import com.ceams.module.ai.config.AiProperties;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** 行政用车汇总只读工具（仅 SYS_ADMIN）。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VehicleUsageTool implements ReadOnlyTool {

    /** 日期参数格式：yyyy-MM-dd。 */
    private static final Pattern DATE_PATTERN = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");

    private static final String INVALID_DATE = "参数格式不正确：%s 必须为 yyyy-MM-dd";

    private final AdminVehicleService adminVehicleService;

    private final AiProperties aiProperties;

    @Override
    public String name() {
        return "queryVehicleUsage";
    }

    @Override
    public String description() {
        return "查询某日期区间的用车汇总：申请数、已通过数、总里程、总费用，以及按车辆分组的明细。"
                + "start、end 必填，格式 yyyy-MM-dd；vehicleId 可选。仅 SYS_ADMIN 角色可调用。";
    }

    @Override
    public Map<String, Object> parameterSchema() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("start", ToolSchemas.text("开始日期，格式 yyyy-MM-dd，必填"));
        properties.put("end", ToolSchemas.text("结束日期，格式 yyyy-MM-dd，必填"));
        properties.put("vehicleId", ToolSchemas.text("车辆 ID，可传数字或数字字符串，可选"));
        return ToolSchemas.object(properties, "start", "end");
    }

    @Override
    public ToolResult execute(Map<String, Object> args) {
        if (!ToolSecurity.isAdmin()) {
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
        Long vehicleId = ToolArgs.longValue(args, "vehicleId");

        VehicleUsageSummaryVO summary =
                adminVehicleService.usageSummary(start, end, vehicleId);
        List<Map<String, Object>> rows = new ArrayList<>();
        if (summary != null) {
            rows.add(toSummaryRow(summary, start, end));
            List<VehicleUsageItemVO> items = summary.getItems();
            if (items != null) {
                for (VehicleUsageItemVO item : items) {
                    rows.add(toItemRow(item));
                }
            }
        }
        return ToolResult.capped(rows, rows.size(), aiProperties.getMaxToolRows());
    }

    /** 汇总行。 */
    private static Map<String, Object> toSummaryRow(
            VehicleUsageSummaryVO vo, LocalDate start, LocalDate end) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("summary", true);
        row.put("start", start.toString());
        row.put("end", end.toString());
        row.put("applyCount", vo.getApplyCount());
        row.put("approvedCount", vo.getApprovedCount());
        row.put("totalMileage", plain(vo.getTotalMileage()));
        row.put("totalExpense", plain(vo.getTotalExpense()));
        return row;
    }

    /** 单台车辆聚合行。 */
    private static Map<String, Object> toItemRow(VehicleUsageItemVO vo) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("vehicleId", vo.getVehicleId() == null ? null : String.valueOf(vo.getVehicleId()));
        row.put("plateNo", vo.getPlateNo());
        row.put("applyCount", vo.getApplyCount());
        row.put("totalMileage", plain(vo.getTotalMileage()));
        row.put("totalExpense", plain(vo.getTotalExpense()));
        return row;
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

    /** 金额/里程转明文字符串，避免科学计数法。 */
    private static String plain(BigDecimal value) {
        return value == null ? null : value.toPlainString();
    }
}
