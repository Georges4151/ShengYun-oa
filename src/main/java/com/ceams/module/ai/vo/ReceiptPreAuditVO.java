package com.ceams.module.ai.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

/** 报销预审结果视图对象（仅提示，不改变审批状态）。 */
@Data
public class ReceiptPreAuditVO {

    /** 票据 ID（雪花 ID 序列化为字符串）。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long receiptId;

    /** 票据编号。 */
    private String receiptNumber;

    /** 报票人姓名。 */
    private String reporterName;

    /** 归属项目 ID（雪花 ID 序列化为字符串）。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long projectId;

    /** 金额。 */
    private BigDecimal amount;

    /** 票据日期。 */
    private LocalDateTime receiptDate;

    /** 汇总风险级别：HIGH / MEDIUM / LOW / NONE。 */
    private String riskLevel;

    /** 命中的风险提示列表（未命中时为空列表）。 */
    private List<PreAuditWarningVO> warnings;
}
