package com.ceams.module.ai.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.module.expense.enums.ReceiptApprovalStatus;
import com.ceams.module.expense.service.ReceiptService;
import com.ceams.module.expense.vo.ReceiptVO;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PendingApprovalsToolTest {

    @Mock private ReceiptService receiptService;

    private PendingApprovalsTool tool;

    @BeforeEach
    void setUp() {
        tool = new PendingApprovalsTool(receiptService, ToolTestSupport.properties());
    }

    @AfterEach
    void tearDown() {
        ToolTestSupport.logout();
    }

    @Test
    void financeSeesAllPendingReceipts() {
        ToolTestSupport.login(ToolTestSupport.USER_ID, "FINANCE");
        ReceiptVO first = receipt("R-1", "100");
        ReceiptVO second = receipt("R-2", "200");
        when(receiptService.pendingApprovals(isNull(), isNull(), isNull(), eq(19)))
                .thenReturn(
                        new ReceiptService.PendingApprovalPage(
                                2L, new BigDecimal("300"), List.of(first, second)));

        ToolResult result = tool.execute(Map.of());

        assertEquals(3, result.rows().size());
        Map<String, Object> summary = result.rows().get(0);
        assertEquals(2L, summary.get("pendingCount"));
        assertEquals(new BigDecimal("300"), summary.get("pendingAmount"));
        assertEquals("R-1", result.rows().get(1).get("receiptNumber"));
        assertFalse(result.truncated());
    }

    @Test
    void staffOnlySeesOwnPendingReceipts() {
        ToolTestSupport.login(ToolTestSupport.USER_ID, "STAFF");
        when(receiptService.pendingApprovals(
                        eq(ToolTestSupport.USER_ID), isNull(), isNull(), eq(19)))
                .thenReturn(new ReceiptService.PendingApprovalPage(0L, BigDecimal.ZERO, List.of()));

        ToolResult result = tool.execute(Map.of());

        assertEquals(1, result.rows().size());
        verify(receiptService)
                .pendingApprovals(eq(ToolTestSupport.USER_ID), isNull(), isNull(), eq(19));
    }

    @Test
    void invalidProjectIdThrowsParameterException() {
        BusinessException ex =
                assertThrows(
                        BusinessException.class, () -> tool.execute(Map.of("projectId", "abc")));
        assertEquals(ErrorCode.VALID_ILLEGAL_ARGUMENT, ex.getErrorCode());
        verifyNoInteractions(receiptService);
    }

    @Test
    void missingIdentityReturnsDeniedRow() {
        ToolTestSupport.logout();
        ToolResult result = tool.execute(Map.of());

        assertEquals(1, result.rows().size());
        assertFalse((Boolean) result.rows().get(0).get("permitted"));
        assertTrue(result.rows().get(0).containsKey("reason"));
        verifyNoInteractions(receiptService);
    }

    private static ReceiptVO receipt(String number, String amount) {
        ReceiptVO receipt = new ReceiptVO();
        receipt.setId(number.equals("R-1") ? 1L : 2L);
        receipt.setReceiptNumber(number);
        receipt.setAmount(new BigDecimal(amount));
        receipt.setApprovalStatus(ReceiptApprovalStatus.PENDING);
        receipt.setCurrentApprovalLevel(1);
        receipt.setReceiptDate(LocalDateTime.of(2026, 11, 5, 9, 0));
        return receipt;
    }
}
