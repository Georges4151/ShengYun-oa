package com.ceams.module.ai.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.common.response.PageResult;
import com.ceams.module.project.enums.BudgetStatus;
import com.ceams.module.project.service.BudgetService;
import com.ceams.module.project.service.ProjectService;
import com.ceams.module.project.vo.BudgetVO;
import com.ceams.module.project.vo.ProjectVO;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BudgetUsageToolTest {

    @Mock private BudgetService budgetService;

    @Mock private ProjectService projectService;

    private BudgetUsageTool tool;

    @BeforeEach
    void setUp() {
        tool = new BudgetUsageTool(budgetService, projectService, ToolTestSupport.properties());
    }

    @AfterEach
    void tearDown() {
        ToolTestSupport.logout();
    }

    @Test
    void staffSeesOwnProjectBudgetUsage() {
        ToolTestSupport.login(ToolTestSupport.USER_ID, "STAFF");
        BudgetVO budget = new BudgetVO();
        budget.setId(1L);
        budget.setBudgetName("主体工程");
        budget.setProjectId(10L);
        budget.setAmount(new BigDecimal("1000"));
        budget.setUsedAmount(new BigDecimal("400"));
        budget.setRemainingAmount(new BigDecimal("600"));
        budget.setStatus(BudgetStatus.ACTIVE);
        when(budgetService.pageForManager(1L, 20L, null, ToolTestSupport.USER_ID))
                .thenReturn(new PageResult<>(List.of(budget), 1L, 1L, 20L));
        ProjectVO project = new ProjectVO();
        project.setId(10L);
        project.setProjectName("滨江改造");
        when(projectService.detail(10L)).thenReturn(project);

        ToolResult result = tool.execute(Map.of());

        assertEquals(1, result.rows().size());
        Map<String, Object> row = result.rows().get(0);
        assertEquals("滨江改造", row.get("projectName"));
        assertEquals(new BigDecimal("40.00"), row.get("usageRate"));
        assertEquals(false, row.get("overBudget"));
        verify(budgetService).pageForManager(1L, 20L, null, ToolTestSupport.USER_ID);
    }

    @Test
    void invalidLimitThrowsParameterException() {
        BusinessException ex =
                assertThrows(BusinessException.class, () -> tool.execute(Map.of("limit", "abc")));
        assertEquals(ErrorCode.VALID_ILLEGAL_ARGUMENT, ex.getErrorCode());
        verifyNoInteractions(budgetService, projectService);
    }

    @Test
    void missingIdentityReturnsDeniedRow() {
        ToolTestSupport.logout();
        ToolResult result = tool.execute(Map.of());

        assertEquals(1, result.rows().size());
        assertFalse((Boolean) result.rows().get(0).get("permitted"));
        verifyNoInteractions(budgetService);
    }
}
