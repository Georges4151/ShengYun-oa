package com.ceams.module.bank.vo;

import com.ceams.module.bank.enums.BankStatementStatus;
import com.ceams.module.bank.enums.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

/** 单条银行流水的匹配候选视图对象（只读建议，不自动确认对账）。 */
@Data
public class BankMatchCandidatesVO {

    /** 银行流水 ID（雪花 ID 序列化为字符串）。 */
    private String statementId;

    /** 付款账户 ID（雪花 ID 序列化为字符串）。 */
    private String accountId;

    /** 流水对账状态。 */
    private BankStatementStatus statementStatus;

    /** 交易日期。 */
    private LocalDateTime transactionDate;

    /** 流水金额。 */
    private BigDecimal amount;

    /** 收支类型。 */
    private TransactionType type;

    /** 流水摘要。 */
    private String description;

    /** 对方户名。 */
    private String counterparty;

    /** 银行流水号。 */
    private String bankReference;

    /** 实际生效的日期容差天数。 */
    private int daysTolerance;

    /** 截断前的候选总数。 */
    private int candidateCount;

    /** 是否因 limit 截断。 */
    private boolean truncated;

    /** 只读声明。 */
    private String disclaimer;

    /** 匹配候选列表（已按匹配分排序并截断）。 */
    private List<MatchCandidateVO> candidates;
}
