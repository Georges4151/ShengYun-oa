package com.ceams.module.project.vo;

import com.ceams.module.project.enums.BudgetStatus;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

/** 预算视图对象。 */
@Data
public class BudgetVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private String budgetName;

    private BigDecimal amount;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long projectId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long contractId;

    private LocalDate startDate;

    private LocalDate endDate;

    private BudgetStatus status;

    private BigDecimal usedAmount;

    private BigDecimal remainingAmount;

    private LocalDateTime createTime;
}
