package com.ceams.module.report.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/** 人力成本报表查询参数（月份必填，部门可选）。 */
@Data
public class HrCostQuery {

    /** 核算月（yyyy-MM），必填。 */
    @NotBlank(message = "核算月不能为空")
    @Pattern(regexp = "^\\d{4}-(0[1-9]|1[0-2])$", message = "月份格式必须为 YYYY-MM")
    private String month;

    /** 部门 ID（逻辑引用 sys_dept.id），为空表示全部部门。 */
    private Long deptId;
}
