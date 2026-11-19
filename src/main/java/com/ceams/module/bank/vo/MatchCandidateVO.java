package com.ceams.module.bank.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

/** 系统侧匹配候选（付款交易 / 总账），仅作人工核实建议。 */
@Data
public class MatchCandidateVO {

    /** 候选来源：PAYMENT_TRANSACTION / GENERAL_LEDGER。 */
    private String source;

    /** 候选记录 ID（雪花 ID 序列化为字符串）。 */
    private String sourceId;

    /** 候选金额。 */
    private BigDecimal amount;

    /** 候选交易日期。 */
    private LocalDateTime transactionDate;

    /** 候选摘要。 */
    private String description;

    /** 总账账号（已掩码）；付款交易为 null。 */
    private String reference;

    /** 与银行流水的日期差（绝对值，天）。 */
    private int dateDiffDays;

    /** 金额差（候选金额 - 流水金额）。 */
    private BigDecimal amountDiff;

    /** 匹配分（0~100）。 */
    private int score;

    /** 得分理由（中文短语）。 */
    private List<String> reasons;
}
