package com.ceams.module.ai.tool;

import com.ceams.module.ai.config.AiProperties;
import com.ceams.module.hr.service.HrEmployeeService;
import com.ceams.module.hr.vo.HeadcountDeptVO;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** 部门在职人数只读工具（仅 SYS_ADMIN）。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HrHeadcountTool implements ReadOnlyTool {

    private final HrEmployeeService hrEmployeeService;

    private final AiProperties aiProperties;

    @Override
    public String name() {
        return "queryHrHeadcount";
    }

    @Override
    public String description() {
        return "按部门查询员工人数统计：部门总人数、试用期人数、在职人数、离职人数。"
                + "deptId 可选，不传返回全部部门；仅 SYS_ADMIN 角色可调用。";
    }

    @Override
    public Map<String, Object> parameterSchema() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("deptId", ToolSchemas.text("部门 ID，可传数字或数字字符串，可选"));
        return ToolSchemas.object(properties);
    }

    @Override
    public ToolResult execute(Map<String, Object> args) {
        if (!ToolSecurity.isAdmin()) {
            return ToolResult.denied("无权查询该数据");
        }
        Long deptId = ToolArgs.longValue(args, "deptId");
        List<HeadcountDeptVO> stats = hrEmployeeService.headcountByDept(deptId);
        List<Map<String, Object>> rows = new ArrayList<>();
        if (stats != null) {
            for (HeadcountDeptVO vo : stats) {
                rows.add(toRow(vo));
            }
        }
        return ToolResult.capped(rows, rows.size(), aiProperties.getMaxToolRows());
    }

    /** 单部门人数行：ID 转字符串，计数保持 long。 */
    private static Map<String, Object> toRow(HeadcountDeptVO vo) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("deptId", vo.getDeptId() == null ? null : String.valueOf(vo.getDeptId()));
        row.put("deptName", vo.getDeptName());
        row.put("totalCount", vo.getTotalCount());
        row.put("probationCount", vo.getProbationCount());
        row.put("activeCount", vo.getActiveCount());
        row.put("terminatedCount", vo.getTerminatedCount());
        return row;
    }
}
