package com.ceams.module.ai.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.module.seal.service.ElectronicSealUsageService;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SealUsageToolTest {

    @Mock private ElectronicSealUsageService electronicSealUsageService;

    private SealUsageTool tool;

    @BeforeEach
    void setUp() {
        tool = new SealUsageTool(electronicSealUsageService, ToolTestSupport.properties());
    }

    @AfterEach
    void tearDown() {
        ToolTestSupport.logout();
    }

    @Test
    void financeSeesUsageCountAndUserDistribution() {
        ToolTestSupport.login(ToolTestSupport.USER_ID, "FINANCE");
        when(electronicSealUsageService.summary(any(), any(), isNull(), isNull()))
                .thenReturn(
                        new ElectronicSealUsageService.UsageSummary(
                                5L, Map.of("李四", 2L, "张三", 3L)));

        ToolResult result = tool.execute(Map.of("period", "2026-11"));

        assertEquals(3, result.rows().size());
        Map<String, Object> summary = result.rows().get(0);
        assertEquals(5L, summary.get("usageCount"));
        assertEquals("张三", result.rows().get(1).get("userName"));
        assertEquals(3L, result.rows().get(1).get("usageCount"));
        assertEquals("李四", result.rows().get(2).get("userName"));
        assertTrue(result.totalRows() >= 5);
    }

    @Test
    void invalidSealIdThrowsParameterException() {
        ToolTestSupport.login(ToolTestSupport.USER_ID, "FINANCE");
        BusinessException ex =
                assertThrows(BusinessException.class, () -> tool.execute(Map.of("sealId", "abc")));
        assertEquals(ErrorCode.VALID_ILLEGAL_ARGUMENT, ex.getErrorCode());
        verifyNoInteractions(electronicSealUsageService);
    }

    @Test
    void staffIsDeniedWithoutDataAccess() {
        ToolTestSupport.login(ToolTestSupport.USER_ID, "STAFF");
        ToolResult result = tool.execute(Map.of());

        assertEquals(1, result.rows().size());
        assertFalse((Boolean) result.rows().get(0).get("permitted"));
        verifyNoInteractions(electronicSealUsageService);
    }
}
