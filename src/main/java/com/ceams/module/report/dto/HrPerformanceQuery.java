package com.ceams.module.report.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/** 绩效考核报表查询参数（周期必填，部门可选）。 */
@Data
public class HrPerformanceQuery {

    /** 考核周期（yyyy-MM 或 yyyy-Qn），必填。 */
    @NotBlank(message = "考核周期不能为空")
    @Pattern(
            regexp = "^(\\d{4}-(0[1-9]|1[0-2])|\\d{4}-Q[1-4])$",
            message = "考核周期格式必须为 YYYY-MM 或 YYYY-Qn")
    private String period;

    /** 部门 ID（逻辑引用 sys_dept.id），为空表示全部部门。 */
    private Long deptId;
}
