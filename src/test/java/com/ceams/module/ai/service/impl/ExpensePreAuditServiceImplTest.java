package com.ceams.module.ai.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.common.response.PageResult;
import com.ceams.module.ai.config.AiProperties;
import com.ceams.module.ai.vo.PreAuditWarningVO;
import com.ceams.module.ai.vo.ReceiptPreAuditVO;
import com.ceams.module.expense.enums.VerificationStatus;
import com.ceams.module.expense.service.ReceiptService;
import com.ceams.module.expense.vo.ReceiptVO;
import com.ceams.module.project.service.BudgetService;
import com.ceams.module.project.vo.BudgetVO;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/** {@link ExpensePreAuditServiceImpl} 规则判定与风险聚合单元测试。 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ExpensePreAuditServiceImplTest {

    private static final LocalDateTime RECEIPT_DATE = LocalDateTime.of(2026, 11, 6, 10, 30);

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-11-07T02:00:00Z"), ZoneId.of("Asia/Shanghai"));

    private static final long RECEIPT_ID = 1L;

    private static final long PROJECT_ID = 7L;

    @Mock private ReceiptService receiptService;

    @Mock private BudgetService budgetService;

    private AiProperties properties;

    private ExpensePreAuditServiceImpl service;

    @BeforeEach
    void setUp() {
        properties = new AiProperties();
        service =
                new ExpensePreAuditServiceImpl(
                        properties, receiptService, budgetService, FIXED_CLOCK);
        when(receiptService.findPotentialDuplicates(any(), any(), any(), any(), any()))
                .thenReturn(List.of());
    }

    @Test
    void audit无风险票据返回NONE() {
        ReceiptVO receipt =
                receipt(
                        "R-0001",
                        new BigDecimal("300"),
                        null,
                        "/files/invoice.pdf",
                        VerificationStatus.VERIFIED);
        when(receiptService.detail(RECEIPT_ID)).thenReturn(receipt);

        ReceiptPreAuditVO vo = service.audit(RECEIPT_ID);

        assertEquals("NONE", vo.getRiskLevel());
        assertTrue(vo.getWarnings().isEmpty());
        assertEquals(RECEIPT_ID, vo.getReceiptId());
        assertEquals("R-0001", vo.getReceiptNumber());
    }

    @Test
    void audit单笔金额超阈值返回AMOUNT_THRESHOLD_MEDIUM() {
        ReceiptVO receipt =
                receipt(
                        "R-0002",
                        new BigDecimal("6000"),
                        null,
                        "/files/invoice.pdf",
                        VerificationStatus.VERIFIED);
        when(receiptService.detail(RECEIPT_ID)).thenReturn(receipt);

        ReceiptPreAuditVO vo = service.audit(RECEIPT_ID);

        PreAuditWarningVO warning = warning(vo, "AMOUNT_THRESHOLD");
        assertNotNull(warning);
        assertEquals("MEDIUM", warning.getLevel());
        assertTrue(warning.getMessage().contains("¥6,000.00"));
        assertTrue(warning.getMessage().contains("¥5,000.00"));
        assertEquals("MEDIUM", vo.getRiskLevel());
    }

    @Test
    void audit预算使用率95返回BUDGET_USAGE_MEDIUM() {
        ReceiptVO receipt =
                receipt(
                        "R-0003",
                        new BigDecimal("300"),
                        PROJECT_ID,
                        "/files/invoice.pdf",
                        VerificationStatus.VERIFIED);
        when(receiptService.detail(RECEIPT_ID)).thenReturn(receipt);
        when(budgetService.page(1, 50, PROJECT_ID)).thenReturn(budgetPage("10000", "9500"));

        ReceiptPreAuditVO vo = service.audit(RECEIPT_ID);

        PreAuditWarningVO warning = warning(vo, "BUDGET_USAGE");
        assertNotNull(warning);
        assertEquals("MEDIUM", warning.getLevel());
        assertTrue(warning.getMessage().contains("95.0%"));
        assertEquals("MEDIUM", vo.getRiskLevel());
    }

    @Test
    void audit预算使用率105返回BUDGET_USAGE_HIGH() {
        ReceiptVO receipt =
                receipt(
                        "R-0004",
                        new BigDecimal("300"),
                        PROJECT_ID,
                        "/files/invoice.pdf",
                        VerificationStatus.VERIFIED);
        when(receiptService.detail(RECEIPT_ID)).thenReturn(receipt);
        when(budgetService.page(1, 50, PROJECT_ID)).thenReturn(budgetPage("10000", "10500"));

        ReceiptPreAuditVO vo = service.audit(RECEIPT_ID);

        PreAuditWarningVO warning = warning(vo, "BUDGET_USAGE");
        assertNotNull(warning);
        assertEquals("HIGH", warning.getLevel());
        assertTrue(warning.getMessage().contains("105.0%"));
        assertEquals("HIGH", vo.getRiskLevel());
    }

    @Test
    void audit同票号重复返回DUPLICATE_NUMBER_HIGH() {
        ReceiptVO receipt =
                receipt(
                        "R-0005",
                        new BigDecimal("300"),
                        null,
                        "/files/invoice.pdf",
                        VerificationStatus.VERIFIED);
        when(receiptService.detail(RECEIPT_ID)).thenReturn(receipt);
        when(receiptService.findPotentialDuplicates(
                        eq(RECEIPT_ID), eq("R-0005"), isNull(), isNull(), isNull()))
                .thenReturn(
                        List.of(
                                receipt(
                                        "R-0005",
                                        new BigDecimal("300"),
                                        null,
                                        null,
                                        VerificationStatus.VERIFIED)));

        ReceiptPreAuditVO vo = service.audit(RECEIPT_ID);

        PreAuditWarningVO warning = warning(vo, "DUPLICATE_NUMBER");
        assertNotNull(warning);
        assertEquals("HIGH", warning.getLevel());
        assertEquals("HIGH", vo.getRiskLevel());
        assertFalse(
                vo.getWarnings().stream().anyMatch(w -> "DUPLICATE_AMOUNT".equals(w.getRule())));
    }

    @Test
    void audit同金额同日重复返回DUPLICATE_AMOUNT_MEDIUM() {
        ReceiptVO receipt =
                receipt(
                        "R-0006",
                        new BigDecimal("300"),
                        null,
                        "/files/invoice.pdf",
                        VerificationStatus.VERIFIED);
        when(receiptService.detail(RECEIPT_ID)).thenReturn(receipt);
        when(receiptService.findPotentialDuplicates(
                        eq(RECEIPT_ID), isNull(), eq(new BigDecimal("300")), any(), any()))
                .thenReturn(
                        List.of(
                                receipt(
                                        "R-0007",
                                        new BigDecimal("300"),
                                        null,
                                        null,
                                        VerificationStatus.VERIFIED)));

        ReceiptPreAuditVO vo = service.audit(RECEIPT_ID);

        PreAuditWarningVO warning = warning(vo, "DUPLICATE_AMOUNT");
        assertNotNull(warning);
        assertEquals("MEDIUM", warning.getLevel());
        assertTrue(warning.getMessage().contains("¥300.00"));
        assertEquals("MEDIUM", vo.getRiskLevel());
        assertFalse(
                vo.getWarnings().stream().anyMatch(w -> "DUPLICATE_NUMBER".equals(w.getRule())));
    }

    @Test
    void audit缺发票附件返回MISSING_INVOICE_HIGH() {
        ReceiptVO receipt =
                receipt("R-0008", new BigDecimal("300"), null, "   ", VerificationStatus.VERIFIED);
        when(receiptService.detail(RECEIPT_ID)).thenReturn(receipt);

        ReceiptPreAuditVO vo = service.audit(RECEIPT_ID);

        PreAuditWarningVO warning = warning(vo, "MISSING_INVOICE");
        assertNotNull(warning);
        assertEquals("HIGH", warning.getLevel());
        assertEquals("HIGH", vo.getRiskLevel());
    }

    @Test
    void audit核验状态未通过返回HIGH待核验返回MEDIUM() {
        ReceiptVO rejected =
                receipt(
                        "R-0009",
                        new BigDecimal("300"),
                        null,
                        "/files/invoice.pdf",
                        VerificationStatus.REJECTED);
        when(receiptService.detail(RECEIPT_ID)).thenReturn(rejected);
        ReceiptPreAuditVO rejectedVo = service.audit(RECEIPT_ID);
        PreAuditWarningVO rejectedWarning = warning(rejectedVo, "VERIFICATION_REJECTED");
        assertNotNull(rejectedWarning);
        assertEquals("HIGH", rejectedWarning.getLevel());
        assertEquals("HIGH", rejectedVo.getRiskLevel());

        ReceiptVO pending =
                receipt(
                        "R-0010",
                        new BigDecimal("300"),
                        null,
                        "/files/invoice.pdf",
                        VerificationStatus.PENDING);
        when(receiptService.detail(RECEIPT_ID)).thenReturn(pending);
        ReceiptPreAuditVO pendingVo = service.audit(RECEIPT_ID);
        PreAuditWarningVO pendingWarning = warning(pendingVo, "VERIFICATION_PENDING");
        assertNotNull(pendingWarning);
        assertEquals("MEDIUM", pendingWarning.getLevel());
        assertEquals("MEDIUM", pendingVo.getRiskLevel());
    }

    @Test
    void audit预算查询异常不影响其余规则() {
        ReceiptVO receipt =
                receipt(
                        "R-0011",
                        new BigDecimal("6000"),
                        PROJECT_ID,
                        "/files/invoice.pdf",
                        VerificationStatus.VERIFIED);
        when(receiptService.detail(RECEIPT_ID)).thenReturn(receipt);
        when(budgetService.page(1, 50, PROJECT_ID)).thenThrow(new IllegalStateException("预算服务不可用"));

        ReceiptPreAuditVO vo = service.audit(RECEIPT_ID);

        assertNotNull(warning(vo, "AMOUNT_THRESHOLD"));
        assertFalse(vo.getWarnings().stream().anyMatch(w -> "BUDGET_USAGE".equals(w.getRule())));
        assertEquals("MEDIUM", vo.getRiskLevel());
    }

    @Test
    void audit票据不存在抛BIZ_NOT_FOUND() {
        when(receiptService.detail(99L))
                .thenThrow(BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "票据不存在"));

        BusinessException exception =
                assertThrows(BusinessException.class, () -> service.audit(99L));

        assertEquals(ErrorCode.BIZ_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void pendingList限制边界兜底20超过50取50() {
        ReceiptService.PendingApprovalPage empty =
                new ReceiptService.PendingApprovalPage(0, BigDecimal.ZERO, List.of());
        when(receiptService.pendingApprovals(null, null, null, 20)).thenReturn(empty);
        when(receiptService.pendingApprovals(null, null, null, 50)).thenReturn(empty);

        assertTrue(service.pendingList(0).isEmpty());
        verify(receiptService).pendingApprovals(null, null, null, 20);

        assertTrue(service.pendingList(99).isEmpty());
        verify(receiptService).pendingApprovals(null, null, null, 50);
    }

    @Test
    void pendingList对每张票据输出预审结果() {
        ReceiptVO pendingReceipt =
                receipt("R-0012", new BigDecimal("6000"), null, null, VerificationStatus.PENDING);
        when(receiptService.pendingApprovals(null, null, null, 20))
                .thenReturn(
                        new ReceiptService.PendingApprovalPage(
                                1, new BigDecimal("6000"), List.of(pendingReceipt)));

        List<ReceiptPreAuditVO> result = service.pendingList(20);

        assertEquals(1, result.size());
        ReceiptPreAuditVO vo = result.get(0);
        assertNotNull(warning(vo, "AMOUNT_THRESHOLD"));
        assertNotNull(warning(vo, "MISSING_INVOICE"));
        assertNotNull(warning(vo, "VERIFICATION_PENDING"));
        assertEquals("HIGH", vo.getRiskLevel());
    }

    private static ReceiptVO receipt(
            String receiptNumber,
            BigDecimal amount,
            Long projectId,
            String filePath,
            VerificationStatus verificationStatus) {
        ReceiptVO vo = new ReceiptVO();
        vo.setId(RECEIPT_ID);
        vo.setReceiptNumber(receiptNumber);
        vo.setReporterName("张三");
        vo.setProjectId(projectId);
        vo.setAmount(amount);
        vo.setReceiptDate(RECEIPT_DATE);
        vo.setFilePath(filePath);
        vo.setVerificationStatus(verificationStatus);
        return vo;
    }

    private static PageResult<BudgetVO> budgetPage(String amount, String usedAmount) {
        BudgetVO budget = new BudgetVO();
        budget.setProjectId(PROJECT_ID);
        budget.setAmount(new BigDecimal(amount));
        budget.setUsedAmount(new BigDecimal(usedAmount));
        return new PageResult<>(List.of(budget), 1, 1, 50);
    }

    private static PreAuditWarningVO warning(ReceiptPreAuditVO vo, String rule) {
        return vo.getWarnings().stream()
                .filter(warning -> rule.equals(warning.getRule()))
                .findFirst()
                .orElse(null);
    }
}
