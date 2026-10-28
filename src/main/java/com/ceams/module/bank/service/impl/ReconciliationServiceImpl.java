package com.ceams.module.bank.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.common.response.PageResult;
import com.ceams.module.bank.converter.ReconciliationResultConverter;
import com.ceams.module.bank.dto.ReconciliationMatchRequest;
import com.ceams.module.bank.dto.ReconciliationResolveRequest;
import com.ceams.module.bank.entity.BankStatement;
import com.ceams.module.bank.entity.GeneralLedger;
import com.ceams.module.bank.entity.ReconciliationResult;
import com.ceams.module.bank.enums.BankStatementStatus;
import com.ceams.module.bank.enums.DiscrepancyType;
import com.ceams.module.bank.enums.ReconciliationStatus;
import com.ceams.module.bank.enums.ResolutionStatus;
import com.ceams.module.bank.mapper.BankStatementMapper;
import com.ceams.module.bank.mapper.GeneralLedgerMapper;
import com.ceams.module.bank.mapper.ReconciliationResultMapper;
import com.ceams.module.bank.service.ReconciliationService;
import com.ceams.module.bank.util.PageUtil;
import com.ceams.module.bank.vo.ReconciliationMatchResultVO;
import com.ceams.module.bank.vo.ReconciliationResultVO;
import com.ceams.module.payment.entity.PaymentAccount;
import com.ceams.module.payment.entity.PaymentTransaction;
import com.ceams.module.payment.mapper.PaymentTransactionMapper;
import com.ceams.module.payment.service.PaymentAccountQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 对账结果服务实现：自动匹配（金额 + 日期±N 天为主键，流水号/户名辅助）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReconciliationServiceImpl implements ReconciliationService {

    private static final int DEFAULT_DAYS_TOLERANCE = 3;

    private final ReconciliationResultMapper reconciliationResultMapper;
    private final ReconciliationResultConverter reconciliationResultConverter;
    private final BankStatementMapper bankStatementMapper;
    private final GeneralLedgerMapper generalLedgerMapper;
    private final PaymentTransactionMapper paymentTransactionMapper;
    private final PaymentAccountQueryService paymentAccountQueryService;

    @Override
    public PageResult<ReconciliationResultVO> page(long page, long size, Long accountId,
                                                   ReconciliationStatus status, ResolutionStatus resolutionStatus) {
        Page<ReconciliationResult> p = PageUtil.resolve(page, size);
        LambdaQueryWrapper<ReconciliationResult> wrapper = new LambdaQueryWrapper<>();
        if (accountId != null) {
            wrapper.eq(ReconciliationResult::getAccountId, accountId);
        }
        if (status != null) {
            wrapper.eq(ReconciliationResult::getStatus, status);
        }
        if (resolutionStatus != null) {
            wrapper.eq(ReconciliationResult::getResolutionStatus, resolutionStatus);
        }
        wrapper.orderByDesc(ReconciliationResult::getCreateTime);
        IPage<ReconciliationResult> result = reconciliationResultMapper.selectPage(p, wrapper);
        return new PageResult<>(reconciliationResultConverter.toVOList(result.getRecords()),
                result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public ReconciliationResultVO detail(Long id) {
        return reconciliationResultConverter.toVO(requireResult(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReconciliationMatchResultVO match(ReconciliationMatchRequest request) {
        Long accountId = request.getAccountId();
        if (!paymentAccountQueryService.exists(accountId)) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "付款账户不存在");
        }
        int days = request.getDaysTolerance() == null ? DEFAULT_DAYS_TOLERANCE : request.getDaysTolerance();

        List<BankStatement> statements = bankStatementMapper.selectList(
                new LambdaQueryWrapper<BankStatement>()
                        .eq(BankStatement::getAccountId, accountId)
                        .eq(BankStatement::getStatus, BankStatementStatus.UNRECONCILED));

        List<PaymentTransaction> transactions = paymentTransactionMapper.selectList(
                new LambdaQueryWrapper<PaymentTransaction>()
                        .eq(PaymentTransaction::getAccountId, accountId));

        List<GeneralLedger> ledgers = loadLedgersByAccount(accountId);

        int matched = 0;
        int unmatched = 0;
        int partialMatched = 0;
        Set<Long> matchedTransactionIds = new HashSet<>();

        for (BankStatement statement : statements) {
            Candidate candidate = findBestCandidate(statement, transactions, ledgers, days);
            ReconciliationResult result = new ReconciliationResult();
            result.setAccountId(accountId);
            result.setStatementId(statement.getId());
            result.setBankAmount(statement.getAmount());
            result.setBankDate(statement.getTransactionDate());
            result.setBankDescription(statement.getDescription());
            result.setStatementReference(statement.getBankReference());
            result.setReconciliationDate(LocalDateTime.now());

            if (candidate == null) {
                result.setStatus(ReconciliationStatus.UNMATCHED);
                result.setDiscrepancyType(DiscrepancyType.MISSING_IN_SYSTEM);
                statement.setStatus(BankStatementStatus.RECONCILED);
                unmatched++;
            } else {
                result.setSystemAmount(candidate.amount());
                result.setSystemDate(candidate.date());
                result.setSystemDescription(candidate.description());
                result.setTransactionId(candidate.transactionId());
                result.setTransactionReference(candidate.reference());

                if (candidate.dateDiff() == 0 && candidate.descriptionMatch()) {
                    result.setStatus(ReconciliationStatus.MATCHED);
                    statement.setStatus(BankStatementStatus.MATCHED);
                    if (candidate.transactionId() != null) {
                        matchedTransactionIds.add(candidate.transactionId());
                    }
                    matched++;
                } else if (candidate.dateDiff() != 0) {
                    result.setStatus(ReconciliationStatus.PARTIAL_MATCHED);
                    result.setDiscrepancyType(DiscrepancyType.DATE_MISMATCH);
                    statement.setStatus(BankStatementStatus.RECONCILED);
                    partialMatched++;
                } else {
                    result.setStatus(ReconciliationStatus.PARTIAL_MATCHED);
                    result.setDiscrepancyType(DiscrepancyType.DESCRIPTION_MISMATCH);
                    statement.setStatus(BankStatementStatus.RECONCILED);
                    partialMatched++;
                }
            }
            reconciliationResultMapper.insert(result);
            bankStatementMapper.updateById(statement);
        }

        unmatched += createMissingInBank(accountId, transactions, matchedTransactionIds);

        return new ReconciliationMatchResultVO(matched, unmatched, partialMatched);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resolve(Long id, ReconciliationResolveRequest request) {
        ReconciliationResult result = requireResult(id);
        if (result.getResolutionStatus() == ResolutionStatus.RESOLVED) {
            throw BusinessException.of(ErrorCode.BIZ_STATE_CONFLICT, "对账结果已处理");
        }
        result.setResolutionStatus(ResolutionStatus.RESOLVED);
        result.setResolutionNotes(request.getResolutionNotes());
        result.setReconciliationDate(LocalDateTime.now());
        reconciliationResultMapper.updateById(result);
        log.info("对账结果 {} 已标记为已处理", id);
    }

    private ReconciliationResult requireResult(Long id) {
        ReconciliationResult result = reconciliationResultMapper.selectById(id);
        if (result == null) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "对账结果不存在");
        }
        return result;
    }

    /** 总账无 account_id，通过付款账户名与总账账户名对齐。 */
    private List<GeneralLedger> loadLedgersByAccount(Long accountId) {
        PaymentAccount account = paymentAccountQueryService.getById(accountId);
        if (account == null || StringUtils.isBlank(account.getName())) {
            return List.of();
        }
        return generalLedgerMapper.selectList(
                new LambdaQueryWrapper<GeneralLedger>()
                        .eq(GeneralLedger::getAccountName, account.getName()));
    }

    /** 系统有记录但银行无对应流水 -> 生成 UNMATCHED(MISSING_IN_BANK)。 */
    private int createMissingInBank(Long accountId, List<PaymentTransaction> transactions,
                                    Set<Long> matchedTransactionIds) {
        int created = 0;
        for (PaymentTransaction transaction : transactions) {
            if (matchedTransactionIds.contains(transaction.getId())) {
                continue;
            }
            Long exists = reconciliationResultMapper.selectCount(
                    new LambdaQueryWrapper<ReconciliationResult>()
                            .eq(ReconciliationResult::getTransactionId, transaction.getId()));
            if (exists != null && exists > 0) {
                continue;
            }
            ReconciliationResult result = new ReconciliationResult();
            result.setAccountId(accountId);
            result.setTransactionId(transaction.getId());
            result.setStatus(ReconciliationStatus.UNMATCHED);
            result.setDiscrepancyType(DiscrepancyType.MISSING_IN_BANK);
            result.setSystemAmount(transaction.getAmount());
            result.setSystemDate(transaction.getTransactionDate());
            result.setSystemDescription(transaction.getDescription());
            result.setReconciliationDate(LocalDateTime.now());
            reconciliationResultMapper.insert(result);
            created++;
        }
        return created;
    }

    private Candidate findBestCandidate(BankStatement statement, List<PaymentTransaction> transactions,
                                        List<GeneralLedger> ledgers, int days) {
        Candidate best = null;
        for (PaymentTransaction transaction : transactions) {
            if (!amountEquals(transaction.getAmount(), statement.getAmount())) {
                continue;
            }
            int diff = daysBetween(statement.getTransactionDate(), transaction.getTransactionDate());
            if (diff > days) {
                continue;
            }
            Candidate candidate = new Candidate(transaction.getAmount(), transaction.getTransactionDate(),
                    transaction.getDescription(), transaction.getId(), null, diff,
                    descriptionsMatch(statement.getDescription(), transaction.getDescription()));
            if (isBetter(candidate, best)) {
                best = candidate;
            }
        }
        for (GeneralLedger ledger : ledgers) {
            if (!amountEquals(ledger.getAmount(), statement.getAmount())) {
                continue;
            }
            int diff = daysBetween(statement.getTransactionDate(), ledger.getTransactionTime());
            if (diff > days) {
                continue;
            }
            Candidate candidate = new Candidate(ledger.getAmount(), ledger.getTransactionTime(),
                    ledger.getSummary(), null, ledger.getAccountNumber(), diff,
                    descriptionsMatch(statement.getDescription(), ledger.getSummary()));
            if (isBetter(candidate, best)) {
                best = candidate;
            }
        }
        return best;
    }

    private boolean amountEquals(BigDecimal a, BigDecimal b) {
        if (a == null || b == null) {
            return false;
        }
        return a.compareTo(b) == 0;
    }

    private int daysBetween(LocalDateTime bankDate, LocalDateTime systemDate) {
        if (bankDate == null || systemDate == null) {
            return Integer.MAX_VALUE;
        }
        return (int) Math.abs(ChronoUnit.DAYS.between(bankDate.toLocalDate(), systemDate.toLocalDate()));
    }

    private boolean descriptionsMatch(String bankDesc, String systemDesc) {
        String a = StringUtils.trimToEmpty(bankDesc);
        String b = StringUtils.trimToEmpty(systemDesc);
        if (a.isEmpty() || b.isEmpty()) {
            return true;
        }
        return a.equals(b);
    }

    private boolean isBetter(Candidate candidate, Candidate best) {
        if (best == null) {
            return true;
        }
        if (candidate.dateDiff() != best.dateDiff()) {
            return candidate.dateDiff() < best.dateDiff();
        }
        return candidate.descriptionMatch() && !best.descriptionMatch();
    }

    /**
     * 匹配候选：系统侧（付款交易或总账）的一条记录。
     */
    private record Candidate(BigDecimal amount, LocalDateTime date, String description,
                             Long transactionId, String reference, int dateDiff, boolean descriptionMatch) {
    }
}
