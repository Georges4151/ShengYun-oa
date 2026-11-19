package com.ceams.module.admin.dto;

import com.ceams.module.admin.enums.AssetCategory;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

/** 资产编辑请求（字段为空表示不修改）。 */
@Data
public class AssetUpdateRequest {

    /** 资产 ID。 */
    @NotNull(message = "资产 ID 不能为空")
    private Long id;

    /** 资产名称。 */
    private String name;

    /** 分类。 */
    private AssetCategory category;

    /** 规格型号。 */
    private String specification;

    /** 购置日期。 */
    private LocalDate purchaseDate;

    /** 购置金额。 */
    private BigDecimal purchasePrice;

    /** 归属部门 ID。 */
    private Long deptId;

    /** 保管人 ID。 */
    private Long keeperId;

    /** 存放位置。 */
    private String location;

    /** 折旧月数。 */
    private Integer depreciationMonths;

    /** 备注。 */
    private String remark;
}
