package com.ceams.module.masterdata.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 材料视图对象。
 */
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

    private LocalDateTime createTime;
}