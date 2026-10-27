package com.ceams.module.project.vo;

import com.ceams.module.project.enums.ProjectStatus;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 项目台账视图对象。
 */
@Data
public class ProjectVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private String projectName;

    private String clientName;

    private String projectAddress;

    private String projectType;

    private LocalDate startDate;

    private LocalDate endDate;

    private BigDecimal contractAmount;

    private BigDecimal budgetAmount;

    private BigDecimal actualAmount;

    private ProjectStatus status;

    private String description;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long managerId;

    private String managerName;

    private LocalDateTime createTime;
}