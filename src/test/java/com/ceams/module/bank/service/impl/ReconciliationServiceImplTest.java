package com.ceams.module.bank.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.module.bank.converter.ReconciliationResultConverter;
import com.ceams.module.bank.entity.BankStatement;
import com.ceams.module.bank.entity.GeneralLedger;
import com.ceams.module.bank.enums.BankStatementStatus;
import com.ceams.module.bank.mapper.BankStatementMapper;
import com.ceams.module.bank.mapper.GeneralLedgerMapper;
import com.ceams.module.bank.mapper.ReconciliationResultMapper;
import com.ceams.module.bank.vo.BankMatchCandidatesVO;
import com.ceams.module.bank.vo.MatchCandidateVO;
import com.ceams.module.payment.entity.PaymentAccount;
import com.ceams.module.payment.entity.PaymentTransaction;
import com.ceams.module.payment.mapper.PaymentTransactionMapper;
import com.ceams.module.payment.service.PaymentAccountQueryService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/** {@link ReconciliationServiceImpl#matchCandidates} 只读候选打分与截断测试。 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReconciliationServiceImplTest {

    private static final Long ACCOUNT_ID = 9L;

    private static final Long STATEMENT_ID = 100L;

    private static final LocalDateTime BANK_DATE = LocalDateTime.of(2026, 11, 5, 10, 0);

    @Mock private ReconciliationResultMapper reconciliationResultMapper;
    @Mock private ReconciliationResultConverter reconciliationResultConverter;
    @Mock private BankStatementMapper bankStatementMapper;
    @Mock private GeneralLedgerMapper generalLedgerMapper;
    @Mock private PaymentTransactionMapper paymentTransactionMapper;
    @Mock private PaymentAccountQueryService paymentAccountQueryService;

    private ReconciliationServiceImpl service;

    @BeforeEach
    void setUp() {
        service =
                new ReconciliationServiceImpl(
                        reconciliationResultMapper,
                        reconciliationResultConverter,
                        bankStatementMapper,
                        generalLedgerMapper,
                        paymentTransactionMapper,
                        paymentAccountQueryService);
    }

    @Test
    void 金额日期摘要完全一致时得分最高() {
        BankStatement statement = statement(new BigDecimal("100.00"), "工程款");
        stubPool(
                statement,
                List.of(transaction(1L, new BigDecimal("100.00"), BANK_DATE, "工程款")),
                List.of());

        BankMatchCandidatesVO vo = service.matchCandidates(STATEMENT_ID, 3, 5);

        assertEquals(1, vo.getCandidateCount());
        assertFalse(vo.isTruncated());
        MatchCandidateVO candidate = vo.getCandidates().get(0);
        assertEquals(90, candidate.getScore());
        assertEquals(0, candidate.getDateDiffDays());
        assertEquals("PAYMENT_TRANSACTION", candidate.getSource());
        assertEquals("1", candidate.getSourceId());
        assertEquals(new BigDecimal("0.00"), candidate.getAmountDiff());
        assertTrue(candidate.getReasons().contains("金额完全一致"));
        assertTrue(candidate.getReasons().contains("交易日期一致"));
        assertTrue(candidate.getReasons().contains("摘要完全一致"));
    }

    @Test
    void 日期相差两天按公式扣分() {
        BankStatement statement = statement(new BigDecimal("100.00"), "工程款");
        stubPool(
                statement,
                List.of(transaction(1L, new BigDecimal("100.00"), BANK_DATE.plusDays(2), "工程款")),
                List.of());

        BankMatchCandidatesVO vo = service.matchCandidates(STATEMENT_ID, 3, 5);

        MatchCandidateVO candidate = vo.getCandidates().get(0);
        assertEquals(2, candidate.getDateDiffDays());
        assertEquals(70, candidate.getScore());
        assertTrue(candidate.getReasons().contains("日期相差 2 天"));
    }

    @Test
    void 金额差异百分之三时得到部分金额分() {
        BankStatement statement = statement(new BigDecimal("100.00"), "工程款");
        stubPool(
                statement,
                List.of(transaction(1L, new BigDecimal("103.00"), BANK_DATE, "工程款")),
                List.of());

        BankMatchCandidatesVO vo = service.matchCandidates(STATEMENT_ID, 3, 5);

        MatchCandidateVO candidate = vo.getCandidates().get(0);
        assertEquals(55, candidate.getScore());
        assertEquals(new BigDecimal("3.00"), candidate.getAmountDiff());
        assertTrue(String.join("", candidate.getReasons()).contains("金额差"));
    }

    @Test
    void 总账账号按末四位掩码() {
        BankStatement statement = statement(new BigDecimal("100.00"), "工程款");
        GeneralLedger ledger =
                ledger(2L, "6222021234567890", new BigDecimal("100.00"), BANK_DATE, "工程款");
        stubPool(statement, List.of(), List.of(ledger));

        BankMatchCandidatesVO vo = service.matchCandidates(STATEMENT_ID, 3, 5);

        MatchCandidateVO candidate = vo.getCandidates().get(0);
        assertEquals("GENERAL_LEDGER", candidate.getSource());
        assertEquals("****7890", candidate.getReference());
        assertEquals(90, candidate.getScore());
    }

    @Test
    void 无候选时总数为零且未截断() {
        BankStatement statement = statement(new BigDecimal("100.00"), "工程款");
        stubPool(statement, List.of(), List.of());

        BankMatchCandidatesVO vo = service.matchCandidates(STATEMENT_ID, 3, 5);

        assertEquals(0, vo.getCandidateCount());
        assertFalse(vo.isTruncated());
        assertTrue(vo.getCandidates().isEmpty());
        assertEquals("仅为只读匹配建议，不会自动确认对账，请人工核实", vo.getDisclaimer());
    }

    @Test
    void 流水不存在抛业务不存在异常() {
        when(bankStatementMapper.selectById(STATEMENT_ID)).thenReturn(null);

        BusinessException ex =
                assertThrows(
                        BusinessException.class, () -> service.matchCandidates(STATEMENT_ID, 3, 5));

        assertEquals(ErrorCode.BIZ_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void limit截断候选并标记truncated() {
        BankStatement statement = statement(new BigDecimal("100.00"), "工程款");
        stubPool(
                statement,
                List.of(
                        transaction(1L, new BigDecimal("100.00"), BANK_DATE, "工程款"),
                        transaction(2L, new BigDecimal("100.00"), BANK_DATE, "工程款"),
                        transaction(3L, new BigDecimal("100.00"), BANK_DATE, "工程款")),
                List.of());

        BankMatchCandidatesVO vo = service.matchCandidates(STATEMENT_ID, 3, 2);

        assertEquals(3, vo.getCandidateCount());
        assertTrue(vo.isTruncated());
        assertEquals(2, vo.getCandidates().size());
        assertEquals("1", vo.getCandidates().get(0).getSourceId());
        assertEquals("2", vo.getCandidates().get(1).getSourceId());
    }

    @Test
    void 日期容差越界回退默认三天() {
        BankStatement statement = statement(new BigDecimal("100.00"), "工程款");
        stubPool(
                statement,
                List.of(transaction(1L, new BigDecimal("100.00"), BANK_DATE.plusDays(4), "工程款")),
                List.of());

        BankMatchCandidatesVO vo = service.matchCandidates(STATEMENT_ID, 99, 5);

        assertEquals(3, vo.getDaysTolerance());
        assertEquals(0, vo.getCandidateCount());
    }

    private static BankStatement statement(BigDecimal amount, String description) {
        BankStatement statement = new BankStatement();
        statement.setId(STATEMENT_ID);
        statement.setAccountId(ACCOUNT_ID);
        statement.setTransactionDate(BANK_DATE);
        statement.setAmount(amount);
        statement.setDescription(description);
        statement.setStatus(BankStatementStatus.UNRECONCILED);
        return statement;
    }

    private static PaymentTransaction transaction(
            Long id, BigDecimal amount, LocalDateTime date, String description) {
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setId(id);
        transaction.setAccountId(ACCOUNT_ID);
        transaction.setAmount(amount);
        transaction.setTransactionDate(date);
        transaction.setDescription(description);
        return transaction;
    }

    private static GeneralLedger ledger(
            Long id, String accountNumber, BigDecimal amount, LocalDateTime time, String summary) {
        GeneralLedger ledger = new GeneralLedger();
        ledger.setId(id);
        ledger.setAccountNumber(accountNumber);
        ledger.setAmount(amount);
        ledger.setTransactionTime(time);
        ledger.setSummary(summary);
        return ledger;
    }

    private void stubPool(
            BankStatement statement,
            List<PaymentTransaction> transactions,
            List<GeneralLedger> ledgers) {
        when(bankStatementMapper.selectById(STATEMENT_ID)).thenReturn(statement);
        PaymentAccount account = new PaymentAccount();
        account.setName("基本户");
        when(paymentAccountQueryService.getById(ACCOUNT_ID)).thenReturn(account);
        when(paymentTransactionMapper.selectList(any())).thenReturn(transactions);
        when(generalLedgerMapper.selectList(any())).thenReturn(ledgers);
    }
}
