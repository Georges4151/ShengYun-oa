package com.ceams.module.payment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 新建付款账户。
 */
@Data
public class PaymentAccountCreateRequest {

    @NotBlank(message = "账户名不能为空")
    @Size(max = 100, message = "账户名长度不能超过 100")
    private String name;

    @NotBlank(message = "账户类型不能为空")
    @Size(max = 50, message = "账户类型长度不能超过 50")
    private String type;

    /** 初始余额（可选，缺省为 0）。 */
    @DecimalMin(value = "0", message = "初始余额不能为负")
    private BigDecimal balance;

    /** 账号【需加密（阶段4）】。 */
    @Size(max = 100, message = "账号长度不能超过 100")
    private String accountNumber;

    private String description;
}
