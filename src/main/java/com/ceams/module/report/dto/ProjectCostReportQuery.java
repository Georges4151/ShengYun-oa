package com.ceams.module.report.dto;

import lombok.Data;

/**
 * 项目成本报表查询参数（全部可选）。
 */
@Data
public class ProjectCostReportQuery {

    /** 项目 ID（逻辑引用 biz_project.id），为空时输出全部项目。 */
    private Long projectId;
}
