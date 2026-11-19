package com.ceams.module.bank.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ceams.module.bank.converter.ReconciliationResultConverter;
import com.ceams.module.bank.dto.ReconciliationMatchRequest;
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
import com.ceams.module.bank.vo.BankMatchCandidatesVO;
import com.ceams.module.bank.vo.ReconciliationMatchResultVO;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * {@link ReconciliationServiceImpl#match} 链路级测试：成功匹配落库、重复对账幂等、金额不一致的记录方式，
 * 以及 {@link ReconciliationServiceImpl#matchCandidates} 的只读约定。
 *
 * <p>候选打分细节已由 {@link ReconciliationServiceImplTest} 覆盖，此处不重复。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReconciliationMatchFlowTest {

    private static final Long ACCOUNT_ID = 9L;

    private static final Long STATEMENT_ID = 100L;

    private static final Long TRANSACTION_ID = 200L;

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

    /** 金额、日期、摘要一致 -> MATCHED，流水置 MATCHED，对账记录字段完整落库。 */
    @Test
    void matchShouldPersistMatchedResultAndFlipStatementStatus() {
        BankStatement statement = statement(new BigDecimal("100.00"), "工程款");
        PaymentTransaction transaction =
                transaction(TRANSACTION_ID, new BigDecimal("100.00"), BANK_DATE, "工程款");
        stubPool(statement, List.of(transaction), List.of());
        when(paymentAccountQueryService.exists(ACCOUNT_ID)).thenReturn(true);

        ReconciliationMatchResultVO result = service.match(matchRequest(3));

        assertThat(result.getMatched()).isEqualTo(1);
        assertThat(result.getUnmatched()).isZero();
        assertThat(result.getPartialMatched()).isZero();

        ArgumentCaptor<ReconciliationResult> resultCaptor =
                ArgumentCaptor.forClass(ReconciliationResult.class);
        verify(reconciliationResultMapper).insert(resultCaptor.capture());
        ReconciliationResult saved = resultCaptor.getValue();
        assertThat(saved.getAccountId()).isEqualTo(ACCOUNT_ID);
        assertThat(saved.getStatementId()).isEqualTo(STATEMENT_ID);
        assertThat(saved.getTransactionId()).isEqualTo(TRANSACTION_ID);
        assertThat(saved.getStatus()).isEqualTo(ReconciliationStatus.MATCHED);
        assertThat(saved.getDiscrepancyType()).isNull();
        assertThat(saved.getBankAmount()).isEqualByComparingTo("100.00");
        assertThat(saved.getSystemAmount()).isEqualByComparingTo("100.00");
        assertThat(saved.getBankDate()).isEqualTo(BANK_DATE);
        assertThat(saved.getSystemDate()).isEqualTo(BANK_DATE);
        assertThat(saved.getBankDescription()).isEqualTo("工程款");
        assertThat(saved.getSystemDescription()).isEqualTo("工程款");
        assertThat(saved.getStatementReference()).isEqualTo("BANK-REF-001");
        assertThat(saved.getResolutionStatus()).isEqualTo(ResolutionStatus.UNRESOLVED);
        assertThat(saved.getReconciliationDate()).isNotNull();

        ArgumentCaptor<BankStatement> statementCaptor =
                ArgumentCaptor.forClass(BankStatement.class);
        verify(bankStatementMapper).updateById(statementCaptor.capture());
        assertThat(statementCaptor.getValue().getStatus()).isEqualTo(BankStatementStatus.MATCHED);
        // 已匹配的交易不进入「银行缺失」补录分支
        verify(reconciliationResultMapper, never()).selectCount(any());
    }

    /**
     * 重复对账的幂等行为：生产实现以 status=UNRECONCILED 作为查询条件，已匹配流水不会被再次扫描；
     * 同时「系统有银行无」的补录以 transactionId 的既有对账记录数做去重。
     *
     * <p>说明：该方法不会抛出业务异常，而是返回 0 计数的幂等结果，故此处不涉及错误码断言。
     */
    @Test
    void matchShouldBeIdempotentWhenStatementAlreadyMatched() {
        PaymentTransaction transaction =
                transaction(TRANSACTION_ID, new BigDecimal("100.00"), BANK_DATE, "工程款");
        when(paymentAccountQueryService.exists(ACCOUNT_ID)).thenReturn(true);
        // 模拟数据库按 status=UNRECONCILED 过滤后的结果：已匹配流水不再返回
        when(bankStatementMapper.selectList(any())).thenReturn(List.of());
        when(paymentTransactionMapper.selectList(any())).thenReturn(List.of(transaction));
        when(paymentAccountQueryService.getById(ACCOUNT_ID)).thenReturn(account("基本户"));
        when(generalLedgerMapper.selectList(any())).thenReturn(List.of());
        // 该交易已有对账记录，补录分支应跳过
        when(reconciliationResultMapper.selectCount(any())).thenReturn(1L);

        ReconciliationMatchResultVO result = service.match(matchRequest(3));

        assertThat(result.getMatched()).isZero();
        assertThat(result.getUnmatched()).isZero();
        assertThat(result.getPartialMatched()).isZero();
        verify(reconciliationResultMapper, never()).insert(any(ReconciliationResult.class));
        verify(bankStatementMapper, never()).updateById(any(BankStatement.class));
    }

    /**
     * 金额不一致时不会产生 MATCHED 记录。
     *
     * <p>生产实现的语义是「记录差异」而不是「静默拒绝」：银行流水生成 UNMATCHED/MISSING_IN_SYSTEM，
     * 系统交易生成 UNMATCHED/MISSING_IN_BANK，因此仍会落两条对账记录。此处按实际行为断言，
     * 并显式验证不存在任何 MATCHED 记录与 MATCHED 状态回写。
     */
    @Test
    void matchShouldNotMatchWhenAmountDiffers() {
        BankStatement statement = statement(new BigDecimal("100.00"), "工程款");
        PaymentTransaction transaction =
                transaction(TRANSACTION_ID, new BigDecimal("99.00"), BANK_DATE, "工程款");
        stubPool(statement, List.of(transaction), List.of());
        when(paymentAccountQueryService.exists(ACCOUNT_ID)).thenReturn(true);
        when(reconciliationResultMapper.selectCount(any())).thenReturn(0L);

        ReconciliationMatchResultVO result = service.match(matchRequest(3));

        assertThat(result.getMatched()).isZero();
        assertThat(result.getUnmatched()).isEqualTo(2);
        assertThat(result.getPartialMatched()).isZero();

        ArgumentCaptor<ReconciliationResult> resultCaptor =
                ArgumentCaptor.forClass(ReconciliationResult.class);
        verify(reconciliationResultMapper, times(2)).insert(resultCaptor.capture());
        List<ReconciliationResult> saved = resultCaptor.getAllValues();
        assertThat(saved).noneMatch(r -> r.getStatus() == ReconciliationStatus.MATCHED);
        assertThat(saved.get(0).getStatus()).isEqualTo(ReconciliationStatus.UNMATCHED);
        assertThat(saved.get(0).getDiscrepancyType()).isEqualTo(DiscrepancyType.MISSING_IN_SYSTEM);
        assertThat(saved.get(0).getBankAmount()).isEqualByComparingTo("100.00");
        assertThat(saved.get(0).getStatementId()).isEqualTo(STATEMENT_ID);
        assertThat(saved.get(1).getStatus()).isEqualTo(ReconciliationStatus.UNMATCHED);
        assertThat(saved.get(1).getDiscrepancyType()).isEqualTo(DiscrepancyType.MISSING_IN_BANK);
        assertThat(saved.get(1).getTransactionId()).isEqualTo(TRANSACTION_ID);

        ArgumentCaptor<BankStatement> statementCaptor =
                ArgumentCaptor.forClass(BankStatement.class);
        verify(bankStatementMapper).updateById(statementCaptor.capture());
        assertThat(statementCaptor.getValue().getStatus())
                .isEqualTo(BankStatementStatus.RECONCILED)
                .isNotEqualTo(BankStatementStatus.MATCHED);
    }

    /** 候选接口是硬约定的只读操作：只做 select，不产生任何 insert/update/delete。 */
    @Test
    void matchCandidatesShouldNeverWriteAnyTable() {
        BankStatement statement = statement(new BigDecimal("100.00"), "工程款");
        PaymentTransaction transaction =
                transaction(TRANSACTION_ID, new BigDecimal("100.00"), BANK_DATE, "工程款");
        GeneralLedger ledger = ledger(300L, new BigDecimal("100.00"), BANK_DATE, "工程款");
        when(bankStatementMapper.selectById(STATEMENT_ID)).thenReturn(statement);
        when(paymentTransactionMapper.selectList(any())).thenReturn(List.of(transaction));
        when(paymentAccountQueryService.getById(ACCOUNT_ID)).thenReturn(account("基本户"));
        when(generalLedgerMapper.selectList(any())).thenReturn(List.of(ledger));

        BankMatchCandidatesVO vo = service.matchCandidates(STATEMENT_ID, 3, 5);

        assertThat(vo.getCandidateCount()).isEqualTo(2);
        assertThat(vo.getCandidates()).hasSize(2);
        verify(reconciliationResultMapper, never()).insert(any(ReconciliationResult.class));
        verify(reconciliationResultMapper, never()).updateById(any(ReconciliationResult.class));
        verify(reconciliationResultMapper, never()).update(any(), any());
        verify(reconciliationResultMapper, never()).deleteById(any());
        verify(reconciliationResultMapper, never()).delete(any());
        verify(bankStatementMapper, never()).insert(any(BankStatement.class));
        verify(bankStatementMapper, never()).updateById(any(BankStatement.class));
        verify(bankStatementMapper, never()).update(any(), any());
        verify(bankStatementMapper, never()).deleteById(any());
        verify(paymentTransactionMapper, never()).insert(any(PaymentTransaction.class));
        verify(paymentTransactionMapper, never()).updateById(any(PaymentTransaction.class));
        verify(paymentTransactionMapper, never()).deleteById(any());
        verify(generalLedgerMapper, never()).insert(any(GeneralLedger.class));
        verify(generalLedgerMapper, never()).updateById(any(GeneralLedger.class));
        verify(generalLedgerMapper, never()).deleteById(any());
    }

    private static ReconciliationMatchRequest matchRequest(Integer daysTolerance) {
        ReconciliationMatchRequest request = new ReconciliationMatchRequest();
        request.setAccountId(ACCOUNT_ID);
        request.setDaysTolerance(daysTolerance);
        return request;
    }

    private static BankStatement statement(BigDecimal amount, String description) {
        BankStatement statement = new BankStatement();
        statement.setId(STATEMENT_ID);
        statement.setAccountId(ACCOUNT_ID);
        statement.setTransactionDate(BANK_DATE);
        statement.setAmount(amount);
        statement.setDescription(description);
        statement.setBankReference("BANK-REF-001");
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
            Long id, BigDecimal amount, LocalDateTime time, String summary) {
        GeneralLedger ledger = new GeneralLedger();
        ledger.setId(id);
        ledger.setAccountName("基本户");
        ledger.setAmount(amount);
        ledger.setTransactionTime(time);
        ledger.setSummary(summary);
        return ledger;
    }

    private static PaymentAccount account(String name) {
        PaymentAccount account = new PaymentAccount();
        account.setId(ACCOUNT_ID);
        account.setName(name);
        return account;
    }

    private void stubPool(
            BankStatement statement,
            List<PaymentTransaction> transactions,
            List<GeneralLedger> ledgers) {
        when(bankStatementMapper.selectList(any())).thenReturn(List.of(statement));
        when(paymentTransactionMapper.selectList(any())).thenReturn(transactions);
        when(paymentAccountQueryService.getById(ACCOUNT_ID)).thenReturn(account("基本户"));
        when(generalLedgerMapper.selectList(any())).thenReturn(ledgers);
    }
}
