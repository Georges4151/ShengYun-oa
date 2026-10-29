package com.ceams.module.expense.dto;

import com.ceams.module.expense.enums.PaymentMethod;
import com.ceams.module.expense.enums.ReceiptType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/** 更新票据（仅草稿/驳回态可编辑）。 */
@Data
public class ReceiptUpdateRequest {

    /** 报票人 ID。 */
    private Long reporterId;

    /** 归属项目 ID。 */
    private Long projectId;

    @Size(max = 100, message = "报票人姓名长度不能超过 100")
    private String reporterName;

    private LocalDateTime receiptDate;

    @DecimalMin(value = "0", message = "金额不能为负")
    private BigDecimal amount;

    private ReceiptType type;

    private String description;

    @Size(max = 500, message = "文件路径长度不能超过 500")
    private String filePath;

    @Size(max = 255, message = "文件哈希长度不能超过 255")
    private String fileHash;

    private String ocrData;

    private PaymentMethod paymentMethod;
}
