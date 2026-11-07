package com.ceams.module.ai.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.module.ai.client.OllamaChatResult;
import com.ceams.module.ai.client.OllamaClient;
import com.ceams.module.ai.config.AiProperties;
import com.ceams.module.ai.entity.AiDailySummary;
import com.ceams.module.ai.mapper.AiDailySummaryMapper;
import com.ceams.module.ai.service.AiDailySummaryService.TriggerType;
import com.ceams.module.ai.vo.AiDailySummaryVO;
import com.ceams.module.bank.service.BankStatementService;
import com.ceams.module.expense.service.ReceiptService;
import com.ceams.module.iam.service.UserService;
import com.ceams.module.notification.dto.NotificationCreateRequest;
import com.ceams.module.notification.enums.ContentType;
import com.ceams.module.notification.enums.NotificationPriority;
import com.ceams.module.notification.enums.NotificationType;
import com.ceams.module.notification.service.NotificationService;
import com.ceams.module.report.service.ReportService;
import com.ceams.module.report.vo.FinancialReportItemVO;
import com.ceams.module.report.vo.FinancialReportVO;
import com.ceams.module.seal.service.ElectronicSealUsageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/** {@link AiDailySummaryServiceImpl} 模板生成、覆盖更新、AI 润色与通知投递单元测试。 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AiDailySummaryServiceImplTest {

    private static final LocalDate DATE = LocalDate.of(2026, 11, 6);

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-11-07T02:00:00Z"), ZoneId.of("Asia/Shanghai"));

    @Mock private OllamaClient ollamaClient;
    @Mock private AiDailySummaryMapper summaryMapper;
    @Mock private ReceiptService receiptService;
    @Mock private ReportService reportService;
    @Mock private BankStatementService bankStatementService;
    @Mock private ElectronicSealUsageService sealUsageService;
    @Mock private NotificationService notificationService;
    @Mock private UserService userService;

    private AiProperties properties;

    private AiDailySummaryServiceImpl service;

    @BeforeEach
    void setUp() {
        properties = new AiProperties();
        properties.setModel("qwen2.5:7b");
        properties.setEnabled(false);
        service =
                new AiDailySummaryServiceImpl(
                        properties,
                        ollamaClient,
                        summaryMapper,
                        receiptService,
                        reportService,
                        bankStatementService,
                        sealUsageService,
                        notificationService,
                        userService,
                        new ObjectMapper(),
                        FIXED_CLOCK);
    }

    @Test
    void 模板摘要包含关键数字与暂无兜底() {
        stubMetrics();
        stubInsert();

        AiDailySummaryVO vo = service.generate(DATE, TriggerType.MANUAL);

        assertNotNull(vo);
        assertTrue(vo.getContent().startsWith("CEAMS 经营日报（2026-11-06）"));
        assertTrue(vo.getContent().contains("新增 2 笔"));
        assertTrue(vo.getContent().contains("合计 ¥1,200.00"));
        assertTrue(vo.getContent().contains("待审批 1 笔、已通过 1 笔"));
        assertTrue(vo.getContent().contains("核验状态：暂无"));
        assertTrue(vo.getContent().contains("当前待审批报销 3 笔，合计 ¥500.00"));
        assertTrue(vo.getContent().contains("收入 ¥1,000.00，支出 ¥200.00，净额 +¥800.00"));
        assertTrue(vo.getContent().contains("未对账银行流水 4 笔，合计 ¥300.00"));
        assertTrue(vo.getContent().contains("用印：5 次"));
        assertEquals("TEMPLATE", vo.getModel());
        assertEquals("MANUAL", vo.getTriggerType());
        assertEquals(2, ((Number) vo.getMetrics().get("receiptCount")).intValue());
        assertEquals(
                0,
                new BigDecimal("1200.00")
                        .compareTo((BigDecimal) vo.getMetrics().get("receiptAmount")));
    }

    @Test
    void 同日已存在时覆盖更新且不重复创建通知() {
        stubMetrics();
        AiDailySummary existing = new AiDailySummary();
        existing.setId(2001L);
        existing.setSummaryDate(DATE);
        existing.setPeriodType("DAILY");
        existing.setContent("旧内容");
        existing.setModel("TEMPLATE");
        existing.setTriggerType("SCHEDULED");
        existing.setNotificationId(3001L);
        when(summaryMapper.selectOne(any())).thenReturn(existing);

        AiDailySummaryVO vo = service.generate(DATE, TriggerType.MANUAL);

        verify(summaryMapper).updateById(existing);
        verify(summaryMapper, never()).insert(any(AiDailySummary.class));
        verify(notificationService, never()).createAndPublishToUsers(any(), any());
        assertTrue(vo.getContent().contains("合计 ¥1,200.00"));
        assertEquals("MANUAL", existing.getTriggerType());
        assertEquals(3001L, existing.getNotificationId());
    }

    @Test
    void AI润色数字不符时回退模板() {
        properties.setEnabled(true);
        stubMetrics();
        stubInsert();
        OllamaChatResult result = new OllamaChatResult();
        result.setContent("润色后的日报：新增 2 笔，但金额已被改写。");
        when(ollamaClient.chatStream(anyList(), ArgumentMatchers.<Consumer<String>>isNull()))
                .thenReturn(result);

        AiDailySummaryVO vo = service.generate(DATE, TriggerType.MANUAL);

        assertEquals("TEMPLATE", vo.getModel());
        assertTrue(vo.getContent().contains("合计 ¥1,200.00"));
        assertTrue(vo.getContent().contains("净额 +¥800.00"));
    }

    @Test
    void AI润色数字保全时使用润色文本() {
        properties.setEnabled(true);
        stubMetrics();
        stubInsert();
        String polished =
                "CEAMS 经营日报：今日新增报销 2 笔，合计 ¥1,200.00；"
                        + "待审批 1 笔、已通过 1 笔，核验暂无。"
                        + "当前待审批报销 3 笔，合计 ¥500.00；"
                        + "资金收入 ¥1,000.00、支出 ¥200.00，净额 +¥800.00；"
                        + "未对账流水 4 笔，合计 ¥300.00；用印 5 次。";
        OllamaChatResult result = new OllamaChatResult();
        result.setContent(polished);
        when(ollamaClient.chatStream(anyList(), ArgumentMatchers.<Consumer<String>>isNull()))
                .thenReturn(result);

        AiDailySummaryVO vo = service.generate(DATE, TriggerType.MANUAL);

        assertEquals("qwen2.5:7b", vo.getModel());
        assertEquals(polished, vo.getContent());
    }

    @Test
    void 通知按角色查询结果定向投递() {
        stubMetrics();
        stubInsert();
        when(userService.listActiveUserIdsByRoleCodes(List.of("FINANCE", "SYS_ADMIN")))
                .thenReturn(List.of(11L, 22L));
        when(notificationService.createAndPublishToUsers(any(), any())).thenReturn("3001");

        AiDailySummaryVO vo = service.generate(DATE, TriggerType.MANUAL);

        ArgumentCaptor<NotificationCreateRequest> requestCaptor =
                ArgumentCaptor.forClass(NotificationCreateRequest.class);
        verify(notificationService)
                .createAndPublishToUsers(requestCaptor.capture(), eq(List.of(11L, 22L)));
        NotificationCreateRequest request = requestCaptor.getValue();
        assertEquals("经营日报｜2026-11-06", request.getTitle());
        assertEquals(vo.getContent(), request.getContent());
        assertEquals(ContentType.TEXT, request.getContentType());
        assertEquals(NotificationType.SYSTEM, request.getNotificationType());
        assertEquals(NotificationPriority.NORMAL, request.getPriority());
        assertEquals("CEAMS 系统", request.getSenderName());
        assertEquals("ceams://ai/summary?date=2026-11-06", request.getLinkUrl());
        assertNotNull(request.getValidStartTime());
        assertEquals(3001L, vo.getNotificationId());

        // 先插摘要，再创建通知，最后回填通知 ID，避免插入失败产生孤立通知。
        InOrder inOrder = inOrder(summaryMapper, notificationService);
        inOrder.verify(summaryMapper).insert(any(AiDailySummary.class));
        inOrder.verify(notificationService).createAndPublishToUsers(any(), eq(List.of(11L, 22L)));
        inOrder.verify(summaryMapper).updateById(any(AiDailySummary.class));
    }

    @Test
    void AI润色新增数字时回退模板() {
        stubMetrics();
        stubInsert();
        String template = service.generate(DATE, TriggerType.MANUAL).getContent();

        properties.setEnabled(true);
        String polished = template + "\n本月环比增长 12%。";
        OllamaChatResult result = new OllamaChatResult();
        result.setContent(polished);
        when(ollamaClient.chatStream(anyList(), ArgumentMatchers.<Consumer<String>>isNull()))
                .thenReturn(result);

        AiDailySummaryVO vo = service.generate(DATE, TriggerType.MANUAL);

        assertFalse(AiDailySummaryServiceImpl.preservesNumbers(template, polished, DATE));
        assertEquals("TEMPLATE", vo.getModel());
        assertEquals(template, vo.getContent());
    }

    @Test
    void 补录历史日期时不创建通知() {
        stubMetrics();
        stubInsert();
        when(summaryMapper.selectOne(any())).thenReturn(null);

        service.catchUpMissing();

        verify(summaryMapper, atLeast(1)).insert(any(AiDailySummary.class));
        verify(notificationService, never()).createAndPublishToUsers(any(), any());
    }

    @Test
    void 通知异常不影响摘要生成() {
        stubMetrics();
        stubInsert();
        when(userService.listActiveUserIdsByRoleCodes(List.of("FINANCE", "SYS_ADMIN")))
                .thenReturn(List.of(11L));
        when(notificationService.createAndPublishToUsers(any(), any()))
                .thenThrow(new IllegalStateException("通知服务不可用"));

        AiDailySummaryVO vo = service.generate(DATE, TriggerType.MANUAL);

        assertNotNull(vo);
        assertNull(vo.getNotificationId());
        assertTrue(vo.getContent().contains("合计 ¥1,200.00"));
        verify(summaryMapper).insert(any(AiDailySummary.class));
    }

    @Test
    void 不得生成晚于昨日的摘要() {
        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> service.generate(LocalDate.of(2026, 11, 7), TriggerType.MANUAL));

        assertEquals(ErrorCode.VALID_ILLEGAL_ARGUMENT, exception.getErrorCode());
        verify(summaryMapper, never()).insert(any(AiDailySummary.class));
    }

    @Test
    void 无摘要时latest返回null() {
        when(summaryMapper.selectList(any())).thenReturn(List.of());

        assertNull(service.latest());
    }

    private void stubMetrics() {
        when(receiptService.summary(any(), any(), isNull(), isNull()))
                .thenReturn(
                        new ReceiptService.ExpenseSummary(
                                new BigDecimal("1200.00"),
                                2,
                                Map.of("PENDING", 1L, "APPROVED", 1L),
                                Map.of("UNPAID", 2L),
                                Map.of(),
                                Map.of("MEAL", 2L)));
        when(receiptService.pendingApprovals(isNull(), isNull(), isNull(), eq(20)))
                .thenReturn(
                        new ReceiptService.PendingApprovalPage(
                                3, new BigDecimal("500.00"), List.of()));
        when(reportService.financial(any())).thenReturn(report());
        when(bankStatementService.unmatchedSummary(any(), any(), isNull()))
                .thenReturn(new BankStatementService.UnmatchedSummary(4, new BigDecimal("300.00")));
        when(sealUsageService.summary(any(), any(), isNull(), isNull()))
                .thenReturn(new ElectronicSealUsageService.UsageSummary(5, Map.of()));
    }

    private void stubInsert() {
        when(summaryMapper.insert(any(AiDailySummary.class)))
                .thenAnswer(
                        invocation -> {
                            AiDailySummary entity = invocation.getArgument(0);
                            entity.setId(2001L);
                            return 1;
                        });
    }

    private static FinancialReportVO report() {
        FinancialReportVO report = new FinancialReportVO();
        report.setItems(
                List.of(
                        item("INCOME", "1000.00", "2026-11-06T10:00:00", "ACCOUNT"),
                        item("EXPENSE", "200.00", "2026-11-06T12:00:00", "PAYMENT_TRANSACTION"),
                        item("INCOME", "999.00", "2026-11-05T10:00:00", "ACCOUNT"),
                        item("INCOME", "777.00", "2026-11-06T09:00:00", "BANK_STATEMENT")));
        return report;
    }

    private static FinancialReportItemVO item(
            String type, String amount, String transactionDate, String sourceType) {
        FinancialReportItemVO item = new FinancialReportItemVO();
        item.setType(type);
        item.setAmount(new BigDecimal(amount));
        item.setTransactionDate(LocalDateTime.parse(transactionDate));
        item.setSourceType(sourceType);
        return item;
    }
}
