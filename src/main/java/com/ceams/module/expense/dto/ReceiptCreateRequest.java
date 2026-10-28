package com.ceams.module.expense.dto;

import com.ceams.module.expense.enums.PaymentMethod;
import com.ceams.module.expense.enums.ReceiptType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 新建票据（报销单）。
 */
@Data
public class ReceiptCreateRequest {

    @NotBlank(message = "票据编号不能为空")
    @Size(max = 100, message = "票据编号长度不能超过 100")
    private String receiptNumber;

    /** 上报人 ID（可选，默认取当前登录用户）。 */
    private Long userId;

    /** 报票人 ID（可选）。 */
    private Long reporterId;

    /** 归属项目 ID（可选）。 */
    private Long projectId;

    @Size(max = 100, message = "报票人姓名长度不能超过 100")
    private String reporterName;

    private LocalDateTime receiptDate;

    @NotNull(message = "金额不能为空")
    @DecimalMin(value = "0", message = "金额不能为负")
    private BigDecimal amount;

    @NotNull(message = "票据类型不能为空")
    private ReceiptType type;

    private String description;

    @Size(max = 500, message = "文件路径长度不能超过 500")
    private String filePath;

    @Size(max = 255, message = "文件哈希长度不能超过 255")
    private String fileHash;

    private String ocrData;

    private PaymentMethod paymentMethod;
}
