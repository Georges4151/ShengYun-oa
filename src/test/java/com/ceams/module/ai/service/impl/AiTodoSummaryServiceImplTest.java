package com.ceams.module.ai.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.module.ai.client.OllamaChatResult;
import com.ceams.module.ai.client.OllamaClient;
import com.ceams.module.ai.config.AiProperties;
import com.ceams.module.ai.vo.AiTodoSummaryVO;
import com.ceams.module.bpm.entity.BpmInstance;
import com.ceams.module.bpm.entity.BpmTask;
import com.ceams.module.bpm.enums.BpmDataScope;
import com.ceams.module.bpm.mapper.BpmInstanceMapper;
import com.ceams.module.bpm.mapper.BpmTaskMapper;
import com.ceams.module.bpm.support.BpmSecurityGuard;
import com.ceams.module.iam.entity.SysUser;
import com.ceams.module.iam.mapper.UserMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/** {@link AiTodoSummaryServiceImpl} 统计口径、模板生成、AI 润色与降级单元测试。 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AiTodoSummaryServiceImplTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-11-10T02:00:00Z"), ZoneId.of("Asia/Shanghai"));

    private static final Long CURRENT_USER_ID = 9L;

    @Mock private OllamaClient ollamaClient;
    @Mock private BpmTaskMapper taskMapper;
    @Mock private BpmInstanceMapper instanceMapper;
    @Mock private UserMapper userMapper;
    @Mock private BpmSecurityGuard securityGuard;

    private AiProperties properties;

    private AiTodoSummaryServiceImpl service;

    @BeforeEach
    void setUp() {
        properties = new AiProperties();
        properties.setModel("qwen2.5:7b");
        properties.setEnabled(false);
        service =
                new AiTodoSummaryServiceImpl(
                        properties,
                        ollamaClient,
                        taskMapper,
                        instanceMapper,
                        userMapper,
                        securityGuard,
                        FIXED_CLOCK);
        when(securityGuard.currentUserId()).thenReturn(CURRENT_USER_ID);
        when(securityGuard.resolveDataScope()).thenReturn(BpmDataScope.SELF);
    }

    @Test
    void 汇总待办统计分布与最近到期清单() {
        stubTodoData();

        AiTodoSummaryVO vo = service.summary();

        assertEquals(3, vo.getTotalPending());
        assertEquals(1, vo.getOverdue());
        assertEquals(1, vo.getDueToday());
        assertEquals("TEMPLATE", vo.getModel());
        assertEquals(2, vo.getByBizType().size());
        assertEquals("报销", vo.getByBizType().get(0).getLabel());
        assertEquals(2, vo.getByBizType().get(0).getCount());
        assertEquals("项目变更", vo.getByBizType().get(1).getLabel());
        assertEquals(2, vo.getUpcoming().size());
        assertEquals("张三", vo.getUpcoming().get(0).getInitiatorName());
        assertTrue(vo.getUpcoming().get(0).isOverdue());

        assertTrue(vo.getContent().startsWith("CEAMS 待办摘要（2026-11-10）"));
        assertTrue(vo.getContent().contains("待办总计 3 条，其中已超时 1 条，今日到期 1 条。"));
        assertTrue(vo.getContent().contains("报销 2 条、项目变更 1 条"));
        assertTrue(vo.getContent().contains("最近到期"));
        assertTrue(vo.getContent().contains("发起人 张三"));
        assertTrue(vo.getContent().contains("优先处理 1 条已超时任务"));
        assertTrue(vo.getDisclaimer().contains("系统实时汇总"));
    }

    @Test
    void 无待办时输出暂无兜底() {
        when(taskMapper.selectCount(any())).thenReturn(0L);
        when(taskMapper.selectMaps(any())).thenReturn(List.of());
        when(taskMapper.selectPage(any(), any())).thenReturn(new Page<>(1, 5, false));

        AiTodoSummaryVO vo = service.summary();

        assertEquals(0, vo.getTotalPending());
        assertEquals(0, vo.getOverdue());
        assertTrue(vo.getByBizType().isEmpty());
        assertTrue(vo.getUpcoming().isEmpty());
        assertTrue(vo.getContent().contains("待办总计 0 条"));
        assertTrue(vo.getContent().contains("三、最近到期：暂无。"));
        assertTrue(vo.getContent().contains("当前无待办"));
    }

    @Test
    void AI润色数字不符时回退模板() {
        stubTodoData();
        properties.setEnabled(true);
        OllamaChatResult result = new OllamaChatResult();
        result.setContent("今日共有 99 条待办，请尽快处理。");
        when(ollamaClient.chatStream(anyList(), ArgumentMatchers.<Consumer<String>>isNull()))
                .thenReturn(result);

        AiTodoSummaryVO vo = service.summary();

        assertEquals("TEMPLATE", vo.getModel());
        assertTrue(vo.getContent().contains("待办总计 3 条"));
    }

    @Test
    void AI润色数字保全时使用润色文本() {
        stubTodoData();
        String template = service.summary().getContent();

        properties.setEnabled(true);
        OllamaChatResult result = new OllamaChatResult();
        result.setContent(template);
        when(ollamaClient.chatStream(anyList(), ArgumentMatchers.<Consumer<String>>isNull()))
                .thenReturn(result);

        AiTodoSummaryVO vo = service.summary();

        assertEquals("qwen2.5:7b", vo.getModel());
        assertEquals(template, vo.getContent());
    }

    @Test
    void AI异常时回退模板且不抛出() {
        stubTodoData();
        properties.setEnabled(true);
        when(ollamaClient.chatStream(anyList(), ArgumentMatchers.<Consumer<String>>isNull()))
                .thenThrow(new IllegalStateException("Ollama 不可用"));

        AiTodoSummaryVO vo = service.summary();

        assertEquals("TEMPLATE", vo.getModel());
        assertTrue(vo.getContent().contains("待办总计 3 条"));
    }

    @Test
    void 未登录时拒绝查询() {
        when(securityGuard.currentUserId()).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class, () -> service.summary());

        assertEquals(ErrorCode.AUTH_ACCESS_DENIED, exception.getErrorCode());
    }

    /** 统一准备 3 条待办：报销 2 条（1 条今日 12:00 到期）、项目变更 1 条。 */
    private void stubTodoData() {
        // 每次 summary() 依次查询总数、超时数、今日到期数；按 3 次调用为一轮重复，支持多轮汇总
        when(taskMapper.selectCount(any()))
                .thenReturn(3L, 1L, 1L, 3L, 1L, 1L, 3L, 1L, 1L);
        when(taskMapper.selectMaps(any()))
                .thenReturn(List.of(row(100L, 2L), row(101L, 1L)));
        when(instanceMapper.selectBatchIds(anyCollection()))
                .thenReturn(
                        List.of(
                                instance(100L, "EXPENSE", 7L),
                                instance(101L, "CHANGE_REQUEST", 8L)));
        Page<BpmTask> page = new Page<>(1, 5, false);
        page.setRecords(
                List.of(
                        task(1001L, 100L, LocalDateTime.of(2026, 11, 10, 9, 0)),
                        task(1002L, 101L, LocalDateTime.of(2026, 11, 11, 9, 0))));
        when(taskMapper.selectPage(any(), any())).thenReturn(page);
        when(userMapper.selectBatchIds(anyCollection()))
                .thenReturn(List.of(user(7L, "张三"), user(8L, "李四")));
    }

    private static Map<String, Object> row(Long instanceId, Long count) {
        return Map.of("instanceId", instanceId, "cnt", count);
    }

    private static BpmInstance instance(Long id, String bizType, Long initiatorId) {
        BpmInstance instance = new BpmInstance();
        instance.setId(id);
        instance.setBizType(bizType);
        instance.setInitiatorId(initiatorId);
        return instance;
    }

    private static BpmTask task(Long id, Long instanceId, LocalDateTime dueAt) {
        BpmTask task = new BpmTask();
        task.setId(id);
        task.setInstanceId(instanceId);
        task.setDueAt(dueAt);
        return task;
    }

    private static SysUser user(Long id, String name) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setName(name);
        return user;
    }
}
