package com.ceams.module.project.dto.progresspayment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 新建进度款申请。
 */
@Data
public class ProgressPaymentCreateRequest {

    @NotNull(message = "项目 ID 不能为空")
    private Long projectId;

    @NotBlank(message = "期次不能为空")
    @Size(max = 50, message = "期次长度不能超过 50")
    private String paymentPeriod;

    @NotNull(message = "申请金额不能为空")
    @PositiveOrZero(message = "申请金额不能为负")
    private BigDecimal applicationAmount;

    private LocalDate applicationDate;
}