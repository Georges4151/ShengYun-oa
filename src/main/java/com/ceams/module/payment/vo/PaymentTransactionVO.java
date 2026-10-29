package com.ceams.module.payment.vo;

import com.ceams.module.payment.enums.AccountType;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/** 付款交易视图对象。 */
@Data
public class PaymentTransactionVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long accountId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long receiptId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long projectId;

    private BigDecimal amount;

    private AccountType type;

    private LocalDateTime transactionDate;

    private String description;

    private LocalDateTime createTime;
}
