package com.ceams.module.payment.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/** 付款账户视图对象。 */
@Data
public class PaymentAccountVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private String name;

    private String type;

    private BigDecimal balance;

    /** 账号【需加密（阶段4）】。 */
    private String accountNumber;

    private String description;

    private LocalDateTime createTime;
}
