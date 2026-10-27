package com.ceams.module.project.dto.project;

import com.ceams.module.project.enums.ProjectStatus;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 更新项目请求。
 */
@Data
public class ProjectUpdateRequest {

    @Size(max = 200, message = "项目名长度不能超过 200")
    private String projectName;

    @Size(max = 200, message = "客户名长度不能超过 200")
    private String clientName;

    @Size(max = 500, message = "项目地址长度不能超过 500")
    private String projectAddress;

    @Size(max = 100, message = "项目类型长度不能超过 100")
    private String projectType;

    private LocalDate startDate;

    private LocalDate endDate;

    @PositiveOrZero(message = "合同金额不能为负")
    private BigDecimal contractAmount;

    @PositiveOrZero(message = "预算金额不能为负")
    private BigDecimal budgetAmount;

    private ProjectStatus status;

    private String description;

    private Long managerId;
}