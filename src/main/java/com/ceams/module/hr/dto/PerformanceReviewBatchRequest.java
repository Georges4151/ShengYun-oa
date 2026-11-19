package com.ceams.module.hr.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 按模板 + 周期（可选部门）批量生成绩效考评单请求。 */
@Data
public class PerformanceReviewBatchRequest {

    /** 绩效模板 ID。 */
    @NotNull(message = "绩效模板 ID 不能为空")
    private Long templateId;

    /** 考核周期（2026-11 / 2026-Q4）。 */
    @NotBlank(message = "考核周期不能为空")
    @Size(max = 16, message = "考核周期长度不能超过 16")
    private String period;

    /** 部门 ID，为空表示全员。 */
    private Long deptId;
}
