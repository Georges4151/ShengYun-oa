package com.ceams.module.payment.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新付款账户（不接受 balance，余额仅由付款执行驱动）。
 */
@Data
public class PaymentAccountUpdateRequest {

    @Size(max = 100, message = "账户名长度不能超过 100")
    private String name;

    @Size(max = 50, message = "账户类型长度不能超过 50")
    private String type;

    @Size(max = 100, message = "账号长度不能超过 100")
    private String accountNumber;

    private String description;
}
