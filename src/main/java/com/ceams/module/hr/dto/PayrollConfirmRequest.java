package com.ceams.module.hr.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import lombok.Data;

/** 月度工资核算批量确认请求。 */
@Data
public class PayrollConfirmRequest {

    /** 核算月，格式 yyyy-MM（必填）。 */
    @NotBlank(message = "核算月不能为空")
    @Pattern(regexp = "\\d{4}-\\d{2}", message = "核算月格式应为 yyyy-MM")
    private String calcMonth;

    /** 指定员工 ID 列表（可空：为空表示确认该月全部 DRAFT 记录）。 */
    private List<Long> employeeIds;
}
