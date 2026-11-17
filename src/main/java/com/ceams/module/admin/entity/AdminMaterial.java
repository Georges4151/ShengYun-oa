package com.ceams.module.admin.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ceams.common.base.BaseEntity;
import com.ceams.module.admin.enums.MaterialStatus;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 行政易耗品（admin_material）。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("admin_material")
public class AdminMaterial extends BaseEntity {

    /** 物料编码（唯一）。 */
    private String code;

    /** 物料名称。 */
    private String name;

    /** 规格。 */
    private String specification;

    /** 单位（个 / 盒 / 包）。 */
    private String unit;

    /** 当前库存，禁止为负。 */
    private BigDecimal stockQty;

    /** 安全库存。 */
    private BigDecimal safetyStock;

    /** 参考单价。 */
    private BigDecimal unitPrice;

    /** 状态：ENABLED/DISABLED。 */
    private MaterialStatus status;
}
