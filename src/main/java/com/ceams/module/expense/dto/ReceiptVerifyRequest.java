package com.ceams.module.expense.dto;

import com.ceams.module.expense.enums.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.Data;

/** 财务核验请求。 */
@Data
public class ReceiptVerifyRequest {

    @NotNull(message = "付款账户 ID 不能为空")
    private Long paymentAccountId;

    @NotNull(message = "核验金额不能为空")
    @DecimalMin(value = "0", message = "核验金额不能为负")
    private BigDecimal amount;

    private PaymentMethod paymentMethod;

    @Size(max = 255, message = "备注长度不能超过 255")
    private String remark;
}
