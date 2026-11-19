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
import com.ceams.module.project.enums.ProjectStatus;
import com.ceams.module.project.service.ProjectService;
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
class ProjectSummaryToolTest {

    @Mock private ProjectService projectService;

    private ProjectSummaryTool tool;

    @BeforeEach
    void setUp() {
        tool = new ProjectSummaryTool(projectService, ToolTestSupport.properties());
    }

    @AfterEach
    void tearDown() {
        ToolTestSupport.logout();
    }

    @Test
    void staffOnlyQueriesOwnProjects() {
        ToolTestSupport.login(ToolTestSupport.USER_ID, "STAFF");
        ProjectVO project = new ProjectVO();
        project.setId(10L);
        project.setProjectName("滨江改造");
        project.setClientName("某集团");
        project.setContractAmount(new BigDecimal("1000000"));
        project.setBudgetAmount(new BigDecimal("900000"));
        project.setActualAmount(new BigDecimal("120000"));
        project.setStatus(ProjectStatus.IN_PROGRESS);
        project.setManagerName("张三");
        when(projectService.pageForManager(1L, 20L, null, null, ToolTestSupport.USER_ID))
                .thenReturn(new PageResult<>(List.of(project), 1L, 1L, 20L));

        ToolResult result = tool.execute(Map.of());

        assertEquals(1, result.rows().size());
        Map<String, Object> row = result.rows().get(0);
        assertEquals("10", row.get("projectId"));
        assertEquals("滨江改造", row.get("projectName"));
        assertEquals(new BigDecimal("120000"), row.get("actualAmount"));
        assertEquals("IN_PROGRESS", row.get("status"));
        verify(projectService).pageForManager(1L, 20L, null, null, ToolTestSupport.USER_ID);
    }

    @Test
    void invalidStatusThrowsParameterException() {
        BusinessException ex =
                assertThrows(BusinessException.class, () -> tool.execute(Map.of("status", "BAD")));
        assertEquals(ErrorCode.VALID_ILLEGAL_ARGUMENT, ex.getErrorCode());
        verifyNoInteractions(projectService);
    }

    @Test
    void missingIdentityReturnsDeniedRow() {
        ToolTestSupport.logout();
        ToolResult result = tool.execute(Map.of());

        assertEquals(1, result.rows().size());
        assertFalse((Boolean) result.rows().get(0).get("permitted"));
        verifyNoInteractions(projectService);
    }
}
