package com.ceams.module.ai.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.ceams.module.admin.service.AdminAssetService;
import com.ceams.module.admin.service.AdminRepairService;
import com.ceams.module.admin.service.AdminVehicleService;
import com.ceams.module.ai.client.OllamaClient;
import com.ceams.module.ai.config.AiProperties;
import com.ceams.module.ai.dto.AiChatRequest;
import com.ceams.module.ai.mapper.AiConversationMapper;
import com.ceams.module.ai.mapper.AiMessageMapper;
import com.ceams.module.ai.service.AiAuditService;
import com.ceams.module.ai.service.impl.AiChatServiceImpl;
import com.ceams.module.ai.vo.AiChatSyncVO;
import com.ceams.module.hr.service.HrCheckinService;
import com.ceams.module.hr.service.HrEmployeeService;
import com.ceams.module.hr.service.HrPayrollService;
import com.ceams.module.hr.vo.PayrollCostSummaryVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * M6 只读工具安全测试：白名单注册、工资汇总的角色边界、个人薪资明细拦截。
 *
 * <p>全程 Mockito，不启动 Spring 容器、不连库、不依赖 Ollama。
 */
@ExtendWith(MockitoExtension.class)
class M6ReadOnlyToolSecurityTest {

    private static final Long CONVERSATION_ID = 3001L;

    private static final Long MESSAGE_ID = 5001L;

    @Mock private HrPayrollService hrPayrollService;

    @Mock private OllamaClient ollamaClient;

    @Mock private ToolRegistry toolRegistry;

    @Mock private AiAuditService aiAuditService;

    @Mock private AiConversationMapper conversationMapper;

    @Mock private AiMessageMapper messageMapper;

    private AiProperties properties;

    private PayrollSummaryTool payrollSummaryTool;

    @BeforeEach
    void setUp() {
        properties = ToolTestSupport.properties();
        properties.setEnabled(true);
        properties.setModel("qwen2.5:7b");
        payrollSummaryTool = new PayrollSummaryTool(hrPayrollService, properties);
        ToolTestSupport.login(ToolTestSupport.USER_ID, "STAFF");
    }

    @AfterEach
    void tearDown() {
        ToolTestSupport.logout();
    }

    @Test
    void 六个新工具均可通过白名单校验并注册() {
        List<ReadOnlyTool> tools =
                List.of(
                        new HrHeadcountTool(mock(HrEmployeeService.class), properties),
                        new AttendanceSummaryTool(
                                mock(HrCheckinService.class),
                                mock(HrEmployeeService.class),
                                properties),
                        payrollSummaryTool,
                        new AssetInventoryTool(mock(AdminAssetService.class), properties),
                        new VehicleUsageTool(mock(AdminVehicleService.class), properties),
                        new RepairStatsTool(mock(AdminRepairService.class), properties));

        ToolRegistry registry = new ToolRegistry(tools);
        registry.validate();

        assertEquals(6, registry.all().size());
        assertEquals(16, ToolRegistry.ALLOWED_TOOL_NAMES.size());
        for (String name :
                List.of(
                        "queryHrHeadcount",
                        "queryAttendanceSummary",
                        "queryPayrollSummary",
                        "queryAssetInventory",
                        "queryVehicleUsage",
                        "queryRepairStats")) {
            assertTrue(ToolRegistry.ALLOWED_TOOL_NAMES.contains(name), "白名单缺少工具：" + name);
            assertTrue(registry.find(name).isPresent(), "注册表缺少工具：" + name);
        }
    }

    @Test
    void STAFF查询工资汇总被拒绝且不触达业务服务() {
        ToolResult result = payrollSummaryTool.execute(Map.of("month", "2026-10"));

        assertEquals(1, result.rows().size());
        assertFalse((Boolean) result.rows().get(0).get("permitted"));
        assertEquals("无权查询该数据", result.rows().get(0).get("reason"));
        verifyNoInteractions(hrPayrollService);
    }

    @Test
    void FINANCE查询工资汇总放行并返回聚合数() {
        ToolTestSupport.login(ToolTestSupport.USER_ID, "FINANCE");
        PayrollCostSummaryVO vo = new PayrollCostSummaryVO();
        vo.setMonth("2026-10");
        vo.setHeadcount(2);
        vo.setGrossPay(new BigDecimal("10000.00"));
        vo.setNetPay(new BigDecimal("8000.00"));
        vo.setSocialInsurance(new BigDecimal("1000.00"));
        vo.setHousingFund(new BigDecimal("500.00"));
        vo.setTax(new BigDecimal("500.00"));
        vo.setAvgNetPay(new BigDecimal("4000.00"));
        when(hrPayrollService.costSummary("2026-10", null)).thenReturn(vo);

        ToolResult result = payrollSummaryTool.execute(Map.of("month", "2026-10"));

        assertEquals(1, result.rows().size());
        Map<String, Object> row = result.rows().get(0);
        assertTrue((Boolean) row.get("summary"));
        assertEquals("2026-10", row.get("month"));
        assertEquals(2L, row.get("headcount"));
        assertEquals("10000.00", row.get("grossPay"));
        assertEquals("8000.00", row.get("netPay"));
        assertEquals("1000.00", row.get("socialInsurance"));
        assertEquals("500.00", row.get("housingFund"));
        assertEquals("500.00", row.get("tax"));
        assertEquals("4000.00", row.get("avgNetPay"));
        assertFalse(row.containsKey("employeeName"));
    }

    @Test
    void 个人薪资明细提问被拦截返回固定话术() {
        when(aiAuditService.createConversation(anyString())).thenReturn(CONVERSATION_ID);
        when(aiAuditService.appendMessage(
                        anyLong(), anyString(), anyString(), any(), any(), any(), any(), any()))
                .thenReturn(MESSAGE_ID);
        AiChatServiceImpl service =
                new AiChatServiceImpl(
                        properties,
                        ollamaClient,
                        toolRegistry,
                        aiAuditService,
                        conversationMapper,
                        messageMapper,
                        new ObjectMapper(),
                        Runnable::run);

        AiChatRequest request = new AiChatRequest();
        request.setMessage("我的工资条在哪里查看？");
        AiChatSyncVO result = service.chatSync(request);

        assertEquals(AiChatServiceImpl.SALARY_DETAIL_REFUSAL, result.getAnswer());
        assertEquals(CONVERSATION_ID, result.getConversationId());
        assertEquals(MESSAGE_ID, result.getMessageId());
        verifyNoInteractions(ollamaClient);
        verify(toolRegistry, never()).definitions();
        // user + assistant 两条消息均正常落库
        verify(aiAuditService, times(2))
                .appendMessage(
                        anyLong(), anyString(), anyString(), any(), any(), any(), any(), any());

        // 命中个人明细语义，但工资汇总类问题不应被误拦截
        assertTrue(AiChatServiceImpl.SALARY_DETAIL_PATTERN.matcher("张三的工资是多少").find());
        assertTrue(AiChatServiceImpl.SALARY_DETAIL_PATTERN.matcher("我这个月的个税扣了多少").find());
        assertFalse(
                AiChatServiceImpl.SALARY_DETAIL_PATTERN
                        .matcher("2026-10 的工资汇总和个税合计是多少")
                        .find());
        assertFalse(
                AiChatServiceImpl.SALARY_DETAIL_PATTERN.matcher("本月各部门人数统计").find());
    }
}
