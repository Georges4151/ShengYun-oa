package com.ceams.module.masterdata.dto.material;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.Data;

/** 新建材料请求。 */
@Data
public class MaterialCreateRequest {

    @NotBlank(message = "材料编码不能为空")
    @Size(max = 50, message = "材料编码长度不能超过 50")
    private String materialCode;

    @NotBlank(message = "材料名不能为空")
    @Size(max = 200, message = "材料名长度不能超过 200")
    private String materialName;

    @Size(max = 200, message = "规格长度不能超过 200")
    private String specification;

    @NotBlank(message = "计量单位不能为空")
    @Size(max = 20, message = "计量单位长度不能超过 20")
    private String unit;

    @PositiveOrZero(message = "当前单价不能为负")
    private BigDecimal currentPrice;

    private Long supplierId;

    private Long categoryId;
}
