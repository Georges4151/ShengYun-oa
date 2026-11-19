package com.ceams.module.report.dto;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

/** 行政资产报表查询参数（分类可选）。 */
@Data
public class AdminAssetReportQuery {

    /** 资产分类：ELECTRONIC/FURNITURE/VEHICLE_TOOL/OTHER，为空表示全部分类。 */
    @Pattern(
            regexp = "^(ELECTRONIC|FURNITURE|VEHICLE_TOOL|OTHER)?$",
            message = "资产分类必须为 ELECTRONIC/FURNITURE/VEHICLE_TOOL/OTHER")
    private String category;
}
