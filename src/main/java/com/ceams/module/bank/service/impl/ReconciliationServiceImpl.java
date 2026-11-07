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
import com.ceams.module.bank.vo.BankMatchCandidatesVO;
import com.ceams.module.bank.vo.MatchCandidateVO;
import com.ceams.module.bank.vo.ReconciliationMatchResultVO;
import com.ceams.module.bank.vo.ReconciliationResultVO;
import com.ceams.module.payment.entity.PaymentAccount;
import com.ceams.module.payment.entity.PaymentTransaction;
import com.ceams.module.payment.mapper.PaymentTransactionMapper;
import com.ceams.module.payment.service.PaymentAccountQueryService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 对账结果服务实现：自动匹配（金额 + 日期±N 天为主键，流水号/户名辅助）。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReconciliationServiceImpl implements ReconciliationService {

    private static final int DEFAULT_DAYS_TOLERANCE = 3;

    private static final int MAX_DAYS_TOLERANCE = 30;

    private static final int DEFAULT_CANDIDATE_LIMIT = 5;

    private static final int MAX_CANDIDATE_LIMIT = 10;

    private static final String MATCH_DISCLAIMER = "仅为只读匹配建议，不会自动确认对账，请人工核实";

    private static final String RECONCILED_DISCLAIMER_SUFFIX = "；该流水已对账，候选仅供复核";

    private static final String SOURCE_PAYMENT_TRANSACTION = "PAYMENT_TRANSACTION";

    private static final String SOURCE_GENERAL_LEDGER = "GENERAL_LEDGER";

    private static final BigDecimal AMOUNT_EXACT_SCORE_UNIT = new BigDecimal("0.01");

    private static final BigDecimal AMOUNT_CLOSE_SCORE_UNIT = new BigDecimal("0.05");

    private static final Comparator<MatchCandidateVO> CANDIDATE_ORDER =
            Comparator.comparingInt(MatchCandidateVO::getScore)
                    .reversed()
                    .thenComparingInt(MatchCandidateVO::getDateDiffDays)
                    .thenComparing(
                            MatchCandidateVO::getSourceId,
                            Comparator.nullsLast(Comparator.naturalOrder()));

    private final ReconciliationResultMapper reconciliationResultMapper;
    private final ReconciliationResultConverter reconciliationResultConverter;
    private final BankStatementMapper bankStatementMapper;
    private final GeneralLedgerMapper generalLedgerMapper;
    private final PaymentTransactionMapper paymentTransactionMapper;
    private final PaymentAccountQueryService paymentAccountQueryService;

    @Override
    public PageResult<ReconciliationResultVO> page(
            long page,
            long size,
            Long accountId,
            ReconciliationStatus status,
            ResolutionStatus resolutionStatus) {
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
        return new PageResult<>(
                reconciliationResultConverter.toVOList(result.getRecords()),
                result.getTotal(),
                result.getCurrent(),
                result.getSize());
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
        int days =
                request.getDaysTolerance() == null
                        ? DEFAULT_DAYS_TOLERANCE
                        : request.getDaysTolerance();

        List<BankStatement> statements =
                bankStatementMapper.selectList(
                        new LambdaQueryWrapper<BankStatement>()
                                .eq(BankStatement::getAccountId, accountId)
                                .eq(BankStatement::getStatus, BankStatementStatus.UNRECONCILED));

        List<PaymentTransaction> transactions =
                paymentTransactionMapper.selectList(
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
    public BankMatchCandidatesVO matchCandidates(
            Long statementId, Integer daysTolerance, Integer limit) {
        BankStatement statement = bankStatementMapper.selectById(statementId);
        if (statement == null) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "银行流水不存在");
        }
        int safeDays = normalizeDaysTolerance(daysTolerance);
        int safeLimit = normalizeCandidateLimit(limit);
        List<MatchCandidateVO> candidates = buildMatchCandidates(statement, safeDays);
        int total = candidates.size();
        List<MatchCandidateVO> visible =
                total > safeLimit ? new ArrayList<>(candidates.subList(0, safeLimit)) : candidates;

        BankMatchCandidatesVO vo = new BankMatchCandidatesVO();
        vo.setStatementId(statement.getId() == null ? null : String.valueOf(statement.getId()));
        vo.setAccountId(
                statement.getAccountId() == null ? null : String.valueOf(statement.getAccountId()));
        vo.setStatementStatus(statement.getStatus());
        vo.setTransactionDate(statement.getTransactionDate());
        vo.setAmount(statement.getAmount());
        vo.setType(statement.getType());
        vo.setDescription(statement.getDescription());
        vo.setCounterparty(statement.getCounterparty());
        vo.setBankReference(statement.getBankReference());
        vo.setDaysTolerance(safeDays);
        vo.setCandidateCount(total);
        vo.setTruncated(total > safeLimit);
        vo.setDisclaimer(buildMatchDisclaimer(statement));
        vo.setCandidates(List.copyOf(visible));
        return vo;
    }

    private static int normalizeDaysTolerance(Integer daysTolerance) {
        if (daysTolerance == null || daysTolerance < 0 || daysTolerance > MAX_DAYS_TOLERANCE) {
            return DEFAULT_DAYS_TOLERANCE;
        }
        return daysTolerance;
    }

    private static int normalizeCandidateLimit(Integer limit) {
        if (limit == null || limit < 1 || limit > MAX_CANDIDATE_LIMIT) {
            return DEFAULT_CANDIDATE_LIMIT;
        }
        return limit;
    }

    private static String buildMatchDisclaimer(BankStatement statement) {
        if (statement.getStatus() != null
                && statement.getStatus() != BankStatementStatus.UNRECONCILED) {
            return MATCH_DISCLAIMER + RECONCILED_DISCLAIMER_SUFFIX;
        }
        return MATCH_DISCLAIMER;
    }

    /** 组装候选池：同账户付款交易 + 账户名对齐总账，且日期差在容差内。 */
    private List<MatchCandidateVO> buildMatchCandidates(BankStatement statement, int days) {
        List<MatchCandidateVO> candidates = new ArrayList<>();
        Long accountId = statement.getAccountId();
        LocalDateTime bankDate = statement.getTransactionDate();
        if (accountId == null || bankDate == null) {
            return candidates;
        }
        List<PaymentTransaction> transactions =
                paymentTransactionMapper.selectList(
                        new LambdaQueryWrapper<PaymentTransaction>()
                                .eq(PaymentTransaction::getAccountId, accountId));
        List<GeneralLedger> ledgers = loadLedgersByAccount(accountId);
        for (PaymentTransaction transaction : transactions) {
            if (transaction.getTransactionDate() == null) {
                continue;
            }
            int dateDiff = daysBetween(bankDate, transaction.getTransactionDate());
            if (dateDiff > days) {
                continue;
            }
            candidates.add(
                    buildCandidate(
                            statement,
                            SOURCE_PAYMENT_TRANSACTION,
                            transaction.getId(),
                            transaction.getAmount(),
                            transaction.getTransactionDate(),
                            transaction.getDescription(),
                            null,
                            dateDiff));
        }
        for (GeneralLedger ledger : ledgers) {
            if (ledger.getTransactionTime() == null) {
                continue;
            }
            int dateDiff = daysBetween(bankDate, ledger.getTransactionTime());
            if (dateDiff > days) {
                continue;
            }
            candidates.add(
                    buildCandidate(
                            statement,
                            SOURCE_GENERAL_LEDGER,
                            ledger.getId(),
                            ledger.getAmount(),
                            ledger.getTransactionTime(),
                            ledger.getSummary(),
                            maskAccountNumber(ledger.getAccountNumber()),
                            dateDiff));
        }
        candidates.sort(CANDIDATE_ORDER);
        return candidates;
    }

    private static MatchCandidateVO buildCandidate(
            BankStatement statement,
            String source,
            Long sourceId,
            BigDecimal amount,
            LocalDateTime transactionDate,
            String description,
            String reference,
            int dateDiffDays) {
        List<String> reasons = new ArrayList<>();
        int amountScore = scoreAmount(statement.getAmount(), amount, reasons);
        int dateScore = scoreDate(dateDiffDays, reasons);
        int descriptionScore = scoreDescription(statement.getDescription(), description, reasons);

        MatchCandidateVO candidate = new MatchCandidateVO();
        candidate.setSource(source);
        candidate.setSourceId(sourceId == null ? null : String.valueOf(sourceId));
        candidate.setAmount(amount);
        candidate.setTransactionDate(transactionDate);
        candidate.setDescription(description);
        candidate.setReference(reference);
        candidate.setDateDiffDays(dateDiffDays);
        candidate.setAmountDiff(amountDiff(statement.getAmount(), amount));
        candidate.setScore(Math.min(100, amountScore + dateScore + descriptionScore));
        candidate.setReasons(List.copyOf(reasons));
        return candidate;
    }

    /** 金额得分：完全一致 +50；差异 <=1% +30；<=5% +15；其余 0。 */
    private static int scoreAmount(
            BigDecimal bankAmount, BigDecimal systemAmount, List<String> reasons) {
        if (bankAmount == null || systemAmount == null) {
            reasons.add("金额无法比对");
            return 0;
        }
        if (bankAmount.compareTo(systemAmount) == 0) {
            reasons.add("金额完全一致");
            return 50;
        }
        BigDecimal absDiff = systemAmount.subtract(bankAmount).abs();
        BigDecimal base = bankAmount.abs();
        if (base.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal ratio = absDiff.divide(base, 6, RoundingMode.HALF_UP);
            if (ratio.compareTo(AMOUNT_EXACT_SCORE_UNIT) <= 0) {
                reasons.add("金额差 " + formatAmountDiff(absDiff) + " 元");
                return 30;
            }
            if (ratio.compareTo(AMOUNT_CLOSE_SCORE_UNIT) <= 0) {
                reasons.add("金额差 " + formatAmountDiff(absDiff) + " 元");
                return 15;
            }
        }
        reasons.add("金额差异较大");
        return 0;
    }

    /** 日期得分：同日 +25；否则 max(0, 15 - 5*天数)。 */
    private static int scoreDate(int dateDiffDays, List<String> reasons) {
        if (dateDiffDays == 0) {
            reasons.add("交易日期一致");
            return 25;
        }
        reasons.add("日期相差 " + dateDiffDays + " 天");
        return Math.max(0, 15 - 5 * dateDiffDays);
    }

    /** 摘要得分：完全一致 +15；互相包含且均 >=2 字 +10；均为空 +5；其余 0。 */
    private static int scoreDescription(
            String bankDescription, String systemDescription, List<String> reasons) {
        String bank = StringUtils.trimToEmpty(bankDescription);
        String system = StringUtils.trimToEmpty(systemDescription);
        if (bank.isEmpty() && system.isEmpty()) {
            reasons.add("摘要均为空");
            return 5;
        }
        if (!bank.isEmpty() && bank.equals(system)) {
            reasons.add("摘要完全一致");
            return 15;
        }
        if (bank.length() >= 2
                && system.length() >= 2
                && (bank.contains(system) || system.contains(bank))) {
            reasons.add("摘要均含关键字");
            return 10;
        }
        reasons.add("摘要不一致");
        return 0;
    }

    private static BigDecimal amountDiff(BigDecimal bankAmount, BigDecimal systemAmount) {
        if (bankAmount == null || systemAmount == null) {
            return null;
        }
        return systemAmount.subtract(bankAmount);
    }

    private static String formatAmountDiff(BigDecimal absDiff) {
        return absDiff.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    /** 总账账号掩码：空 -> null；长度 <=4 -> ****；否则 **** + 末 4 位。 */
    private static String maskAccountNumber(String accountNumber) {
        String value = StringUtils.trimToNull(accountNumber);
        if (value == null) {
            return null;
        }
        if (value.length() <= 4) {
            return "****";
        }
        return "****" + value.substring(value.length() - 4);
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
    private int createMissingInBank(
            Long accountId,
            List<PaymentTransaction> transactions,
            Set<Long> matchedTransactionIds) {
        int created = 0;
        for (PaymentTransaction transaction : transactions) {
            if (matchedTransactionIds.contains(transaction.getId())) {
                continue;
            }
            Long exists =
                    reconciliationResultMapper.selectCount(
                            new LambdaQueryWrapper<ReconciliationResult>()
                                    .eq(
                                            ReconciliationResult::getTransactionId,
                                            transaction.getId()));
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

    private Candidate findBestCandidate(
            BankStatement statement,
            List<PaymentTransaction> transactions,
            List<GeneralLedger> ledgers,
            int days) {
        Candidate best = null;
        for (PaymentTransaction transaction : transactions) {
            if (!amountEquals(transaction.getAmount(), statement.getAmount())) {
                continue;
            }
            int diff =
                    daysBetween(statement.getTransactionDate(), transaction.getTransactionDate());
            if (diff > days) {
                continue;
            }
            Candidate candidate =
                    new Candidate(
                            transaction.getAmount(),
                            transaction.getTransactionDate(),
                            transaction.getDescription(),
                            transaction.getId(),
                            null,
                            diff,
                            descriptionsMatch(
                                    statement.getDescription(), transaction.getDescription()));
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
            Candidate candidate =
                    new Candidate(
                            ledger.getAmount(),
                            ledger.getTransactionTime(),
                            ledger.getSummary(),
                            null,
                            ledger.getAccountNumber(),
                            diff,
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
        return (int)
                Math.abs(ChronoUnit.DAYS.between(bankDate.toLocalDate(), systemDate.toLocalDate()));
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

    /** 匹配候选：系统侧（付款交易或总账）的一条记录。 */
    private record Candidate(
            BigDecimal amount,
            LocalDateTime date,
            String description,
            Long transactionId,
            String reference,
            int dateDiff,
            boolean descriptionMatch) {}
}
