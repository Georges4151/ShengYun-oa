package com.ceams.module.project.dto.progresspayment;

import com.ceams.module.project.enums.ApprovalStatus;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

/** 更新进度款请求。 */
@Data
public class ProgressPaymentUpdateRequest {

    @Size(max = 50, message = "期次长度不能超过 50")
    private String paymentPeriod;

    @PositiveOrZero(message = "申请金额不能为负")
    private BigDecimal applicationAmount;

    @PositiveOrZero(message = "批准金额不能为负")
    private BigDecimal approvedAmount;

    @PositiveOrZero(message = "实付金额不能为负")
    private BigDecimal paymentAmount;

    private LocalDate applicationDate;

    private LocalDate approvalDate;

    private LocalDate paymentDate;

    private ApprovalStatus status;
}
