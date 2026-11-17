package com.ceams.module.expense.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ceams.common.base.BaseEntity;
import com.ceams.module.expense.enums.PaymentMethod;
import com.ceams.module.expense.enums.PaymentStatus;
import com.ceams.module.expense.enums.ReceiptApprovalStatus;
import com.ceams.module.expense.enums.ReceiptType;
import com.ceams.module.expense.enums.VerificationStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 票据（报销单）（biz_receipt）。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_receipt")
public class Receipt extends BaseEntity {

    /** 票据编号（唯一）。 */
    private String receiptNumber;

    /** 上报人 ID（逻辑引用 sys_user.id）。 */
    private Long userId;

    /** 报票人 ID（逻辑引用 sys_user.id）。 */
    private Long reporterId;

    /** 归属项目 ID（逻辑引用 biz_project.id）。 */
    private Long projectId;

    /** 报票人姓名（冗余）。 */
    private String reporterName;

    /** 票据日期。 */
    private LocalDateTime receiptDate;

    /** 金额。 */
    private BigDecimal amount;

    /** 类型（代码值）。 */
    private ReceiptType type;

    /** 描述。 */
    private String description;

    /** 票据文件路径。 */
    private String filePath;

    /** 文件哈希。 */
    private String fileHash;

    /** OCR 识别数据。 */
    private String ocrData;

    /** 财务核验状态。 */
    private VerificationStatus verificationStatus;

    /** 核验人 ID（逻辑引用 sys_user.id）。 */
    private Long verifiedBy;

    /** 核验时间。 */
    private LocalDateTime verifiedAt;

    /** 付款状态。 */
    private PaymentStatus paymentStatus;

    /** 已支付金额。 */
    private BigDecimal paidAmount;

    /** 支付时间。 */
    private LocalDateTime paidAt;

    /** 支付人 ID（逻辑引用 sys_user.id）。 */
    private Long paidBy;

    /** 支付方式。 */
    private PaymentMethod paymentMethod;

    /** 当前审批级别。 */
    private Integer currentApprovalLevel;

    /** 审批状态（V5 补充列）。 */
    private ReceiptApprovalStatus approvalStatus;

    /** 关联的 BPM 流程实例 ID（逻辑引用 bpm_instance.id，灰度期可为空）。 */
    private Long bpmInstanceId;
}
