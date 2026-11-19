package com.ceams.module.project.dto.budget;

import com.ceams.module.project.enums.BudgetStatus;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

/** 更新预算请求。 */
@Data
public class BudgetUpdateRequest {

    @Size(max = 255, message = "预算名长度不能超过 255")
    private String budgetName;

    @PositiveOrZero(message = "预算金额不能为负")
    private BigDecimal amount;

    private Long projectId;

    private Long contractId;

    private LocalDate startDate;

    private LocalDate endDate;

    private BudgetStatus status;
}
