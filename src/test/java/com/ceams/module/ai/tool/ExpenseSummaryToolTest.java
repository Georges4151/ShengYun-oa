package com.ceams.module.ai.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.module.expense.service.ReceiptService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExpenseSummaryToolTest {

    @Mock private ReceiptService receiptService;

    private ExpenseSummaryTool tool;

    @BeforeEach
    void setUp() {
        tool = new ExpenseSummaryTool(receiptService);
    }

    @AfterEach
    void tearDown() {
        ToolTestSupport.logout();
    }

    @Test
    void staffSummaryIsScopedToCurrentUser() {
        ToolTestSupport.login(ToolTestSupport.USER_ID, "STAFF");
        ReceiptService.ExpenseSummary summary =
                new ReceiptService.ExpenseSummary(
                        new BigDecimal("100"),
                        1,
                        Map.of("PENDING", 1L),
                        Map.of("UNPAID", 1L),
                        Map.of("PENDING", 1L),
                        Map.of("MEAL", 1L));
        when(receiptService.summary(any(), any(), eq(ToolTestSupport.USER_ID), isNull()))
                .thenReturn(summary);

        ToolResult result = tool.execute(Map.of("period", "2026-11"));

        assertEquals(1, result.rows().size());
        Map<String, Object> row = result.rows().get(0);
        assertEquals("2026-11", row.get("period"));
        assertEquals(new BigDecimal("100"), row.get("totalAmount"));
        assertEquals(1L, row.get("receiptCount"));
        assertEquals(1L, row.get("pendingCount"));
        verify(receiptService).summary(any(), any(), eq(ToolTestSupport.USER_ID), isNull());
    }

    @Test
    void financeSummaryIsUnscoped() {
        ToolTestSupport.login(ToolTestSupport.USER_ID, "FINANCE");
        when(receiptService.summary(
                        any(LocalDateTime.class), any(LocalDateTime.class), isNull(), isNull()))
                .thenReturn(
                        new ReceiptService.ExpenseSummary(
                                BigDecimal.ZERO, 0, Map.of(), Map.of(), Map.of(), Map.of()));

        ToolResult result = tool.execute(Map.of());

        assertEquals(1, result.rows().size());
        assertEquals(0L, result.rows().get(0).get("pendingCount"));
        verify(receiptService).summary(any(), any(), isNull(), isNull());
    }

    @Test
    void invalidPeriodThrowsParameterException() {
        BusinessException ex =
                assertThrows(
                        BusinessException.class, () -> tool.execute(Map.of("period", "2026-13")));
        assertEquals(ErrorCode.VALID_ILLEGAL_ARGUMENT, ex.getErrorCode());
        verifyNoInteractions(receiptService);
    }

    @Test
    void missingIdentityReturnsDeniedRow() {
        ToolTestSupport.logout();
        ToolResult result = tool.execute(Map.of());

        assertEquals(1, result.rows().size());
        assertFalse((Boolean) result.rows().get(0).get("permitted"));
        verifyNoInteractions(receiptService);
    }
}
