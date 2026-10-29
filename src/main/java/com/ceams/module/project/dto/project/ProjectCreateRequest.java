package com.ceams.module.project.dto.project;

import com.ceams.module.project.enums.ProjectStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

/** 新建项目请求。 */
@Data
public class ProjectCreateRequest {

    @NotBlank(message = "项目名不能为空")
    @Size(max = 200, message = "项目名长度不能超过 200")
    private String projectName;

    @NotBlank(message = "客户名不能为空")
    @Size(max = 200, message = "客户名长度不能超过 200")
    private String clientName;

    @Size(max = 500, message = "项目地址长度不能超过 500")
    private String projectAddress;

    @NotBlank(message = "项目类型不能为空")
    @Size(max = 100, message = "项目类型长度不能超过 100")
    private String projectType;

    @NotNull(message = "开工日期不能为空")
    private LocalDate startDate;

    private LocalDate endDate;

    @NotNull(message = "合同金额不能为空")
    @PositiveOrZero(message = "合同金额不能为负")
    private BigDecimal contractAmount;

    @NotNull(message = "预算金额不能为空")
    @PositiveOrZero(message = "预算金额不能为负")
    private BigDecimal budgetAmount;

    @NotNull(message = "项目状态不能为空")
    private ProjectStatus status;

    private String description;

    private Long managerId;
}
