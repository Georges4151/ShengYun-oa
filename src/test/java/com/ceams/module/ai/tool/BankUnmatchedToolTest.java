package com.ceams.module.ai.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.common.response.PageResult;
import com.ceams.module.bank.enums.BankStatementStatus;
import com.ceams.module.bank.enums.TransactionType;
import com.ceams.module.bank.service.BankStatementService;
import com.ceams.module.bank.vo.BankStatementVO;
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
class BankUnmatchedToolTest {

    @Mock private BankStatementService bankStatementService;

    private BankUnmatchedTool tool;

    @BeforeEach
    void setUp() {
        tool = new BankUnmatchedTool(bankStatementService, ToolTestSupport.properties());
    }

    @AfterEach
    void tearDown() {
        ToolTestSupport.logout();
    }

    @Test
    void financeReturnsExactCountAndAmount() {
        ToolTestSupport.login(ToolTestSupport.USER_ID, "FINANCE");
        when(bankStatementService.unmatchedSummary(any(), any(), isNull()))
                .thenReturn(new BankStatementService.UnmatchedSummary(2L, new BigDecimal("250")));
        BankStatementVO statement = new BankStatementVO();
        statement.setId(1L);
        statement.setAccountId(9L);
        statement.setTransactionDate(LocalDateTime.of(2026, 11, 5, 10, 0));
        statement.setAmount(new BigDecimal("100"));
        statement.setType(TransactionType.INCOME);
        statement.setDescription("工程款");
        statement.setCounterparty("某集团");
        statement.setStatus(BankStatementStatus.UNRECONCILED);
        when(bankStatementService.page(
                        anyLong(),
                        anyLong(),
                        isNull(),
                        eq(BankStatementStatus.UNRECONCILED),
                        isNull(),
                        any(),
                        any()))
                .thenReturn(new PageResult<>(List.of(statement), 2L, 1L, 19L));

        ToolResult result = tool.execute(Map.of("period", "2026-11"));

        assertEquals(2, result.rows().size());
        Map<String, Object> summary = result.rows().get(0);
        assertEquals(2L, summary.get("unmatchedCount"));
        assertEquals(new BigDecimal("250"), summary.get("unmatchedAmount"));
        assertEquals("100", String.valueOf(result.rows().get(1).get("amount")));
        assertTrue(result.truncated());
    }

    @Test
    void invalidAccountIdThrowsParameterException() {
        ToolTestSupport.login(ToolTestSupport.USER_ID, "FINANCE");
        BusinessException ex =
                assertThrows(
                        BusinessException.class, () -> tool.execute(Map.of("accountId", "abc")));
        assertEquals(ErrorCode.VALID_ILLEGAL_ARGUMENT, ex.getErrorCode());
        verifyNoInteractions(bankStatementService);
    }

    @Test
    void staffIsDeniedWithoutDataAccess() {
        ToolTestSupport.login(ToolTestSupport.USER_ID, "STAFF");
        ToolResult result = tool.execute(Map.of());

        assertEquals(1, result.rows().size());
        assertFalse((Boolean) result.rows().get(0).get("permitted"));
        verifyNoInteractions(bankStatementService);
    }
}
