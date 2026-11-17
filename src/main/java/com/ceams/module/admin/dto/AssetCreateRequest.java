package com.ceams.module.admin.dto;

import com.ceams.module.admin.enums.AssetCategory;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

/** 资产建档请求。 */
@Data
public class AssetCreateRequest {

    /** 资产名称。 */
    @NotBlank(message = "资产名称不能为空")
    private String name;

    /** 分类，为空按 OTHER 处理。 */
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
