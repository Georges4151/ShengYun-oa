package com.ceams.module.project.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ceams.common.base.BaseEntity;
import com.ceams.module.project.enums.ProjectStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 项目台账（biz_project）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_project")
public class Project extends BaseEntity {

    /** 项目名。 */
    private String projectName;

    /** 客户名。 */
    private String clientName;

    /** 项目地址。 */
    private String projectAddress;

    /** 项目类型（代码值）。 */
    private String projectType;

    /** 开工日期。 */
    private LocalDateTime startDate;

    /** 竣工日期。 */
    private LocalDateTime endDate;

    /** 合同金额。 */
    private BigDecimal contractAmount;

    /** 预算金额。 */
    private BigDecimal budgetAmount;

    /** 实际已发生金额。 */
    private BigDecimal actualAmount = BigDecimal.ZERO;

    /** 项目状态。 */
    private ProjectStatus status;

    /** 描述。 */
    private String description;

    /** 项目经理 ID（逻辑引用 sys_user.id）。 */
    private Long managerId;

    /** 项目经理姓名（冗余）。 */
    private String managerName;
}