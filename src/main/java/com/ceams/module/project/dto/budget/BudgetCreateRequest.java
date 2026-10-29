package com.ceams.module.project.dto.budget;

import com.ceams.module.project.enums.BudgetStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

/** 新建预算请求。 */
@Data
public class BudgetCreateRequest {

    @NotBlank(message = "预算名不能为空")
    @Size(max = 255, message = "预算名长度不能超过 255")
    private String budgetName;

    @NotNull(message = "预算金额不能为空")
    @PositiveOrZero(message = "预算金额不能为负")
    private BigDecimal amount;

    private Long projectId;

    private Long contractId;

    private LocalDate startDate;

    private LocalDate endDate;

    @NotNull(message = "预算状态不能为空")
    private BudgetStatus status;
}
