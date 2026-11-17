package com.ceams.module.hr.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 初始化员工年度假期额度请求。 */
@Data
public class LeaveQuotaInitRequest {

    @NotNull(message = "员工ID不能为空")
    private Long employeeId;

    @NotNull(message = "年度不能为空")
    private Integer year;
}
