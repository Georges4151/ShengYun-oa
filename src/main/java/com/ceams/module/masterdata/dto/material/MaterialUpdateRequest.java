package com.ceams.module.masterdata.dto.material;

import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 更新材料请求（材料编码不可修改）。
 */
@Data
public class MaterialUpdateRequest {

    @Size(max = 200, message = "材料名长度不能超过 200")
    private String materialName;

    @Size(max = 200, message = "规格长度不能超过 200")
    private String specification;

    @Size(max = 20, message = "计量单位长度不能超过 20")
    private String unit;

    @PositiveOrZero(message = "当前单价不能为负")
    private BigDecimal currentPrice;

    private Long supplierId;

    private Long categoryId;
}