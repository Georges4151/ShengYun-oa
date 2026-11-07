package com.ceams.module.ai.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.common.response.PageResult;
import com.ceams.module.project.enums.ApprovalStatus;
import com.ceams.module.project.service.ProgressPaymentService;
import com.ceams.module.project.service.ProjectService;
import com.ceams.module.project.vo.ProgressPaymentVO;
import com.ceams.module.project.vo.ProjectVO;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReceivableToolTest {

    @Mock private ProgressPaymentService progressPaymentService;

    @Mock private ProjectService projectService;

    private ReceivableTool tool;

    @BeforeEach
    void setUp() {
        tool =
                new ReceivableTool(
                        progressPaymentService, projectService, ToolTestSupport.properties());
    }

    @AfterEach
    void tearDown() {
        ToolTestSupport.logout();
    }

    @Test
    void returnsOutstandingAndOverdueFlag() {
        ToolTestSupport.login(ToolTestSupport.USER_ID, "FINANCE");
        ProgressPaymentVO payment = new ProgressPaymentVO();
        payment.setProjectId(10L);
        payment.setPaymentPeriod("第 2 期");
        payment.setApplicationAmount(new BigDecimal("100"));
        payment.setApprovedAmount(new BigDecimal("80"));
        payment.setPaymentAmount(new BigDecimal("50"));
        payment.setStatus(ApprovalStatus.APPROVED);
        payment.setApprovalDate(LocalDate.now().minusDays(60));
        when(progressPaymentService.page(1L, 20L, null, null))
                .thenReturn(new PageResult<>(List.of(payment), 1L, 1L, 20L));
        ProjectVO project = new ProjectVO();
        project.setId(10L);
        project.setProjectName("滨江改造");
        when(projectService.detail(10L)).thenReturn(project);

        ToolResult result = tool.execute(Map.of());

        assertEquals(1, result.rows().size());
        Map<String, Object> row = result.rows().get(0);
        assertEquals("30", String.valueOf(row.get("outstandingAmount")));
        assertEquals("滨江改造", row.get("projectName"));
        assertTrue((Boolean) row.get("overdue"));
    }

    @Test
    void invalidProjectIdThrowsParameterException() {
        BusinessException ex =
                assertThrows(
                        BusinessException.class, () -> tool.execute(Map.of("projectId", "abc")));
        assertEquals(ErrorCode.VALID_ILLEGAL_ARGUMENT, ex.getErrorCode());
        verifyNoInteractions(progressPaymentService, projectService);
    }

    @Test
    void missingIdentityReturnsDeniedRow() {
        ToolTestSupport.logout();
        ToolResult result = tool.execute(Map.of());

        assertEquals(1, result.rows().size());
        assertFalse((Boolean) result.rows().get(0).get("permitted"));
        verifyNoInteractions(progressPaymentService);
    }

    @Test
    void staffUsesApplicantScope() {
        ToolTestSupport.login(ToolTestSupport.USER_ID, "STAFF");
        when(progressPaymentService.pageForApplicant(1L, 20L, null, null, ToolTestSupport.USER_ID))
                .thenReturn(new PageResult<>(List.of(), 0L, 1L, 20L));

        ToolResult result = tool.execute(Map.of());

        assertEquals(0, result.rows().size());
        verify(progressPaymentService)
                .pageForApplicant(1L, 20L, null, null, ToolTestSupport.USER_ID);
    }
}
