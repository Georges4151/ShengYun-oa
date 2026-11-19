package com.ceams.module.admin.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ceams.common.base.BaseEntity;
import com.ceams.module.admin.enums.AssetCategory;
import com.ceams.module.admin.enums.AssetStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 行政资产台账（admin_asset）。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("admin_asset")
public class AdminAsset extends BaseEntity {

    /** 资产编号（服务端生成 AST-yyyyMM-####，唯一）。 */
    private String assetNo;

    /** 资产名称。 */
    private String name;

    /** 分类：ELECTRONIC/FURNITURE/VEHICLE_TOOL/OTHER。 */
    private AssetCategory category;

    /** 规格型号。 */
    private String specification;

    /** 购置日期。 */
    private LocalDate purchaseDate;

    /** 购置金额。 */
    private BigDecimal purchasePrice;

    /** 归属部门 ID（逻辑引用 sys_dept.id）。 */
    private Long deptId;

    /** 保管人 ID（sys_user.id）。 */
    private Long keeperId;

    /** 存放位置。 */
    private String location;

    /** 状态：IN_STOCK/BORROWED/REPAIRING/SCRAPPED，仅回调与报废 / 盘点接口写入。 */
    private AssetStatus status;

    /** 折旧月数。 */
    private Integer depreciationMonths;

    /** 备注。 */
    private String remark;
}
