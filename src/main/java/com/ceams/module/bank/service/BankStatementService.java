package com.ceams.module.bank.service;

import com.ceams.common.response.PageResult;
import com.ceams.module.bank.dto.BankStatementCreateRequest;
import com.ceams.module.bank.dto.BankStatementUpdateRequest;
import com.ceams.module.bank.enums.BankStatementStatus;
import com.ceams.module.bank.enums.TransactionType;
import com.ceams.module.bank.vo.BankStatementVO;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 银行流水服务。 */
public interface BankStatementService {

    PageResult<BankStatementVO> page(
            long page,
            long size,
            Long accountId,
            BankStatementStatus status,
            TransactionType type,
            LocalDateTime startDate,
            LocalDateTime endDate);

    BankStatementVO detail(Long id);

    String create(BankStatementCreateRequest request);

    void update(Long id, BankStatementUpdateRequest request);

    void delete(Long id);

    /** 期间未对账银行流水汇总（只读）：笔数与金额合计。 */
    UnmatchedSummary unmatchedSummary(
            LocalDateTime startDate, LocalDateTime endDate, Long accountId);

    /** 未对账银行流水汇总结果。 */
    record UnmatchedSummary(long unmatchedCount, BigDecimal totalAmount) {}
}
