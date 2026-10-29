package com.ceams.module.expense.vo;

import com.ceams.module.expense.enums.PaymentMethod;
import com.ceams.module.expense.enums.PaymentStatus;
import com.ceams.module.expense.enums.ReceiptApprovalStatus;
import com.ceams.module.expense.enums.ReceiptType;
import com.ceams.module.expense.enums.VerificationStatus;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/** 票据视图对象。 */
@Data
public class ReceiptVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private String receiptNumber;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long reporterId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long projectId;

    private String reporterName;

    private LocalDateTime receiptDate;

    private BigDecimal amount;

    private ReceiptType type;

    private String description;

    private String filePath;

    private String fileHash;

    private String ocrData;

    private VerificationStatus verificationStatus;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long verifiedBy;

    private LocalDateTime verifiedAt;

    private PaymentStatus paymentStatus;

    private BigDecimal paidAmount;

    private LocalDateTime paidAt;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long paidBy;

    private PaymentMethod paymentMethod;

    private Integer currentApprovalLevel;

    private ReceiptApprovalStatus approvalStatus;

    private LocalDateTime createTime;
}
