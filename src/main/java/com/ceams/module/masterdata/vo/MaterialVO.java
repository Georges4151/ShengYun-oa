package com.ceams.module.masterdata.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/** 材料视图对象。 */
@Data
public class MaterialVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private String materialCode;

    private String materialName;

    private String specification;

    private String unit;

    private BigDecimal currentPrice;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long supplierId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long categoryId;

    /** 供应商名称（展示用，逻辑引用 biz_contact.name，可空）。 */
    private String supplierName;

    /** 分类名称（展示用，逻辑引用 biz_category.name，可空）。 */
    private String categoryName;

    private LocalDateTime createTime;
}
