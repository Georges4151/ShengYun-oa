package com.ceams.module.expense.vo;

import com.ceams.module.expense.enums.PaymentMethod;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 财务核验视图对象。
 */
@Data
public class ReceiptVerificationVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long receiptId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long paymentAccountId;

    private BigDecimal amount;

    private PaymentMethod paymentMethod;

    private LocalDateTime verificationTime;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long verifiedBy;

    private String remark;

    private LocalDateTime createTime;
}
