package com.ceams.module.report.dto;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

/** 审批效率报表查询参数（时间范围与业务类型均可选）。 */
@Data
public class BpmEfficiencyQuery {

    /** 起始日期（yyyy-MM-dd），为空表示不限。 */
    @Pattern(regexp = "^$|^\\d{4}-\\d{2}-\\d{2}$", message = "起始日期格式必须为 yyyy-MM-dd")
    private String start;

    /** 结束日期（yyyy-MM-dd），为空表示不限。 */
    @Pattern(regexp = "^$|^\\d{4}-\\d{2}-\\d{2}$", message = "结束日期格式必须为 yyyy-MM-dd")
    private String end;

    /** 业务类型（bpm_instance.biz_type），为空表示全部。 */
    private String bizType;
}
