package com.ceams.module.masterdata.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ceams.common.base.BaseEntity;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 材料库（biz_material）。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_material")
public class Material extends BaseEntity {

    /** 材料编码。 */
    private String materialCode;

    /** 材料名。 */
    private String materialName;

    /** 规格。 */
    private String specification;

    /** 计量单位。 */
    private String unit;

    /** 当前单价。 */
    private BigDecimal currentPrice;

    /** 供应商 ID（逻辑引用 biz_contact.id）。 */
    private Long supplierId;

    /** 分类 ID（逻辑引用 biz_category.id）。 */
    private Long categoryId;
}
