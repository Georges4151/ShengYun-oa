package com.ceams.module.report.dto;

import lombok.Data;

/** 材料报表查询参数（全部可选）。 */
@Data
public class MaterialReportQuery {

    /** 材料 ID（逻辑引用 biz_material.id）。 */
    private Long materialId;

    /** 分类 ID（逻辑引用 biz_category.id）。 */
    private Long categoryId;
}
