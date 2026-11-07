package com.ceams.module.ai.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.module.report.service.ReportService;
import com.ceams.module.report.vo.FinancialReportItemVO;
import com.ceams.module.report.vo.FinancialReportVO;
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
class CashflowToolTest {

    @Mock private ReportService reportService;

    private CashflowTool tool;

    @BeforeEach
    void setUp() {
        tool = new CashflowTool(reportService, ToolTestSupport.properties());
    }

    @AfterEach
    void tearDown() {
        ToolTestSupport.logout();
    }

    @Test
    void financeReturnsTotalsAndTopCategories() {
        ToolTestSupport.login(ToolTestSupport.USER_ID, "FINANCE");
        FinancialReportItemVO income = item("ACCOUNT", "INCOME", "100", "工程款");
        FinancialReportItemVO expense = item("PAYMENT_TRANSACTION", "EXPENSE", "40", "材料款");
        FinancialReportItemVO ignored = item("BANK_STATEMENT", "EXPENSE", "999", "银行流水");
        FinancialReportVO report = new FinancialReportVO();
        report.setItems(List.of(income, expense, ignored));
        when(reportService.financial(any())).thenReturn(report);

        ToolResult result = tool.execute(Map.of("period", "2026-11"));

        assertEquals(3, result.rows().size());
        Map<String, Object> summary = result.rows().get(0);
        assertEquals(new BigDecimal("100"), summary.get("totalIncome"));
        assertEquals(new BigDecimal("40"), summary.get("totalExpense"));
        assertEquals(new BigDecimal("60"), summary.get("netAmount"));
        Map<String, Object> topExpense = result.rows().get(1);
        assertEquals("材料款", topExpense.get("category"));
        assertEquals("EXPENSE", topExpense.get("type"));
        assertEquals(new BigDecimal("40"), topExpense.get("amount"));
        Map<String, Object> topIncome = result.rows().get(2);
        assertEquals("工程款", topIncome.get("category"));
    }

    @Test
    void invalidPeriodThrowsParameterException() {
        ToolTestSupport.login(ToolTestSupport.USER_ID, "FINANCE");
        BusinessException ex =
                assertThrows(
                        BusinessException.class, () -> tool.execute(Map.of("period", "2026-13")));
        assertEquals(ErrorCode.VALID_ILLEGAL_ARGUMENT, ex.getErrorCode());
        verifyNoInteractions(reportService);
    }

    @Test
    void periodOverTwentyFourMonthsIsRejected() {
        ToolTestSupport.login(ToolTestSupport.USER_ID, "FINANCE");
        BusinessException ex =
                assertThrows(
                        BusinessException.class,
                        () -> tool.execute(Map.of("period", "2020-01~2026-12")));
        assertEquals(ErrorCode.VALID_ILLEGAL_ARGUMENT, ex.getErrorCode());
        verifyNoInteractions(reportService);
    }

    @Test
    void staffIsDeniedWithoutDataAccess() {
        ToolTestSupport.login(ToolTestSupport.USER_ID, "STAFF");
        ToolResult result = tool.execute(Map.of());

        assertEquals(1, result.rows().size());
        assertFalse((Boolean) result.rows().get(0).get("permitted"));
        verifyNoInteractions(reportService);
    }

    private static FinancialReportItemVO item(
            String sourceType, String type, String amount, String description) {
        FinancialReportItemVO item = new FinancialReportItemVO();
        item.setSourceType(sourceType);
        item.setType(type);
        item.setAmount(new BigDecimal(amount));
        item.setDescription(description);
        item.setTransactionDate(LocalDateTime.of(2026, 11, 5, 10, 0));
        return item;
    }
}
