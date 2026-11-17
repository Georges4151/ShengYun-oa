package com.ceams.module.hr.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import lombok.Data;

/** 月度工资核算初始化 / 重算请求。 */
@Data
public class PayrollCalcRequest {

    /** 核算月，格式 yyyy-MM（必填）。 */
    @NotBlank(message = "核算月不能为空")
    @Pattern(regexp = "\\d{4}-\\d{2}", message = "核算月格式应为 yyyy-MM")
    private String calcMonth;

    /** 部门 ID（可空：为空表示全部部门）。 */
    private Long deptId;

    /** 指定员工 ID 列表（可空：为空表示部门内全部在职员工）。 */
    private List<Long> employeeIds;
}
