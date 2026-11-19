package com.ceams.module.payment.vo;

import com.ceams.module.payment.enums.TargetType;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/** 成本分摊视图对象。 */
@Data
public class CostAllocationVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long accountRecordId;

    private TargetType targetType;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long targetId;

    private BigDecimal allocationAmount;

    private BigDecimal allocationRatio;

    private LocalDateTime createTime;
}
