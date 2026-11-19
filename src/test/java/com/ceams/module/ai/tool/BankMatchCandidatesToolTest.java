package com.ceams.module.ai.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.module.bank.service.ReconciliationService;
import com.ceams.module.bank.vo.BankMatchCandidatesVO;
import com.ceams.module.bank.vo.MatchCandidateVO;
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

/** {@link BankMatchCandidatesTool} 权限、行数与降级行为测试。 */
@ExtendWith(MockitoExtension.class)
class BankMatchCandidatesToolTest {

    @Mock private ReconciliationService reconciliationService;

    private BankMatchCandidatesTool tool;

    @BeforeEach
    void setUp() {
        tool = new BankMatchCandidatesTool(reconciliationService, ToolTestSupport.properties());
    }

    @AfterEach
    void tearDown() {
        ToolTestSupport.logout();
    }

    @Test
    void staffIsDeniedWithoutDataAccess() {
        ToolTestSupport.login(ToolTestSupport.USER_ID, "STAFF");

        ToolResult result = tool.execute(Map.of("statementId", "1"));

        assertEquals(1, result.rows().size());
        assertFalse((Boolean) result.rows().get(0).get("permitted"));
        verifyNoInteractions(reconciliationService);
    }

    @Test
    void financeReceivesSummaryAndCandidateRows() {
        ToolTestSupport.login(ToolTestSupport.USER_ID, "FINANCE");
        BankMatchCandidatesVO vo = new BankMatchCandidatesVO();
        vo.setStatementId("1");
        vo.setCandidateCount(1);
        vo.setDaysTolerance(3);
        vo.setTruncated(false);
        vo.setDisclaimer("仅为只读匹配建议，不会自动确认对账，请人工核实");
        MatchCandidateVO candidate = new MatchCandidateVO();
        candidate.setSource("PAYMENT_TRANSACTION");
        candidate.setSourceId("2");
        candidate.setTransactionDate(LocalDateTime.of(2026, 11, 5, 10, 0));
        candidate.setAmount(new BigDecimal("100.00"));
        candidate.setAmountDiff(new BigDecimal("0.00"));
        candidate.setDateDiffDays(0);
        candidate.setScore(90);
        candidate.setReasons(List.of("金额完全一致", "交易日期一致"));
        candidate.setDescription("工程款");
        vo.setCandidates(List.of(candidate));
        when(reconciliationService.matchCandidates(eq(1L), eq(3), eq(5))).thenReturn(vo);

        ToolResult result =
                tool.execute(Map.of("statementId", "1", "daysTolerance", 3, "limit", 5));

        assertEquals(2, result.rows().size());
        Map<String, Object> summary = result.rows().get(0);
        assertTrue((Boolean) summary.get("summary"));
        assertEquals("1", summary.get("statementId"));
        assertEquals(1, summary.get("candidateCount"));
        assertEquals(false, summary.get("truncated"));
        assertEquals(3, summary.get("daysTolerance"));
        Map<String, Object> row = result.rows().get(1);
        assertEquals("PAYMENT_TRANSACTION", row.get("source"));
        assertEquals(90, row.get("score"));
        assertEquals("金额完全一致；交易日期一致", row.get("reasons"));
        assertFalse(result.truncated());
    }

    @Test
    void missingStatementReturnsEmptyResult() {
        ToolTestSupport.login(ToolTestSupport.USER_ID, "FINANCE");
        when(reconciliationService.matchCandidates(
                        anyLong(), nullable(Integer.class), nullable(Integer.class)))
                .thenThrow(BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "银行流水不存在"));

        ToolResult result = tool.execute(Map.of("statementId", "1"));

        assertTrue(result.rows().isEmpty());
        assertEquals(0, result.totalRows());
    }

    @Test
    void blankStatementIdIsRejected() {
        ToolTestSupport.login(ToolTestSupport.USER_ID, "FINANCE");

        BusinessException ex = assertThrows(BusinessException.class, () -> tool.execute(Map.of()));

        assertEquals(ErrorCode.VALID_ILLEGAL_ARGUMENT, ex.getErrorCode());
        verifyNoInteractions(reconciliationService);
    }
}
