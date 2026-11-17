package com.ceams.module.admin.vo;

import com.ceams.module.admin.enums.AssetCategory;
import com.ceams.module.admin.enums.AssetStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

/** 资产台账展示对象。 */
@Data
public class AssetVO {

    /** 资产 ID。 */
    private Long id;

    /** 资产编号。 */
    private String assetNo;

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

    /** 状态。 */
    private AssetStatus status;

    /** 折旧月数。 */
    private Integer depreciationMonths;

    /** 备注。 */
    private String remark;

    /** 创建时间。 */
    private LocalDateTime createTime;
}
