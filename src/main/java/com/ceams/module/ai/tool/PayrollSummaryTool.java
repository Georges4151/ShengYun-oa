package com.ceams.module.ai.tool;

import com.ceams.module.ai.config.AiProperties;
import com.ceams.module.hr.service.HrPayrollService;
import com.ceams.module.hr.vo.PayrollCostDeptVO;
import com.ceams.module.hr.vo.PayrollCostSummaryVO;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 月度工资（人力成本）汇总只读工具。
 *
 * <p>仅输出聚合口径：人数、应发、实发、社保、公积金、个税、人均实发，永不返回个人薪资明细。
 * 权限：SYS_ADMIN / FINANCE 可查全量，其余角色一律拒绝。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PayrollSummaryTool implements ReadOnlyTool {

    /** 月份参数格式：yyyy-MM。 */
    private static final Pattern MONTH_PATTERN = Pattern.compile("\\d{4}-\\d{2}");

    private final HrPayrollService hrPayrollService;

    private final AiProperties aiProperties;

    @Override
    public String name() {
        return "queryPayrollSummary";
    }

    @Override
    public String description() {
        return "查询某核算月的工资（人力成本）汇总：人数、应发合计、实发合计、社保合计、公积金合计、"
                + "个税合计、人均实发，以及按部门分组的聚合数。month 必填，格式 yyyy-MM；"
                + "deptId 可选。仅 FINANCE / SYS_ADMIN 可调用；不提供任何个人薪资明细。";
    }

    @Override
    public Map<String, Object> parameterSchema() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("month", ToolSchemas.text("核算月，格式 yyyy-MM，必填"));
        properties.put("deptId", ToolSchemas.text("部门 ID，可传数字或数字字符串，可选"));
        return ToolSchemas.object(properties, "month");
    }

    @Override
    public ToolResult execute(Map<String, Object> args) {
        if (!ToolSecurity.isFinanceOrAdmin()) {
            return ToolResult.denied("无权查询该数据");
        }
        String month = ToolArgs.text(args, "month");
        if (month == null || !MONTH_PATTERN.matcher(month).matches()) {
            return ToolResult.denied("参数格式不正确：month 必须为 yyyy-MM");
        }
        try {
            YearMonth.parse(month);
        } catch (DateTimeParseException e) {
            return ToolResult.denied("参数格式不正确：month 必须为 yyyy-MM");
        }
        Long deptId = ToolArgs.longValue(args, "deptId");

        PayrollCostSummaryVO summary = hrPayrollService.costSummary(month, deptId);
        List<Map<String, Object>> rows = new ArrayList<>();
        if (summary != null) {
            rows.add(toSummaryRow(summary, month));
            List<PayrollCostDeptVO> byDept = summary.getByDept();
            if (byDept != null) {
                for (PayrollCostDeptVO dept : byDept) {
                    rows.add(toDeptRow(dept));
                }
            }
        }
        return ToolResult.capped(rows, rows.size(), aiProperties.getMaxToolRows());
    }

    /** 汇总行：仅聚合数，金额用 toPlainString 规避科学计数法。 */
    private static Map<String, Object> toSummaryRow(PayrollCostSummaryVO vo, String fallbackMonth) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("summary", true);
        row.put("month", vo.getMonth() == null ? fallbackMonth : vo.getMonth());
        row.put("headcount", vo.getHeadcount());
        row.put("grossPay", plain(vo.getGrossPay()));
        row.put("netPay", plain(vo.getNetPay()));
        row.put("socialInsurance", plain(vo.getSocialInsurance()));
        row.put("housingFund", plain(vo.getHousingFund()));
        row.put("tax", plain(vo.getTax()));
        row.put("avgNetPay", plain(vo.getAvgNetPay()));
        return row;
    }

    /** 部门聚合行：只有部门维度合计，不含员工姓名与个人薪资。 */
    private static Map<String, Object> toDeptRow(PayrollCostDeptVO vo) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("byDept", true);
        row.put("deptId", vo.getDeptId() == null ? null : String.valueOf(vo.getDeptId()));
        row.put("deptName", vo.getDeptName());
        row.put("headcount", vo.getHeadcount());
        row.put("grossPay", plain(vo.getGrossPay()));
        row.put("netPay", plain(vo.getNetPay()));
        row.put("socialInsurance", plain(vo.getSocialInsurance()));
        row.put("housingFund", plain(vo.getHousingFund()));
        row.put("tax", plain(vo.getTax()));
        row.put("avgNetPay", plain(vo.getAvgNetPay()));
        return row;
    }

    /** 金额转明文字符串；null 保持 null，避免模型把 0 当成真实数据。 */
    private static String plain(BigDecimal value) {
        return value == null ? null : value.toPlainString();
    }
}
