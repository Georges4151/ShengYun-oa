package com.ceams.module.expense.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ceams.common.base.BaseEntity;
import com.ceams.module.expense.enums.PaymentMethod;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 财务核验（biz_receipt_verification）。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_receipt_verification")
public class ReceiptVerification extends BaseEntity {

    /** 票据 ID（逻辑引用 biz_receipt.id）。 */
    private Long receiptId;

    /** 付款账户 ID（逻辑引用 biz_payment_account.id）。 */
    private Long paymentAccountId;

    /** 核验金额。 */
    private BigDecimal amount;

    /** 支付方式。 */
    private PaymentMethod paymentMethod;

    /** 核验时间。 */
    private LocalDateTime verificationTime;

    /** 核验人 ID（逻辑引用 sys_user.id）。 */
    private Long verifiedBy;

    /** 备注。 */
    private String remark;
}
