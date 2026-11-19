package com.ceams.module.bpm.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.module.bpm.config.BpmProperties;
import com.ceams.module.bpm.entity.BpmInstance;
import com.ceams.module.bpm.entity.BpmTask;
import com.ceams.module.bpm.enums.BpmTaskStatus;
import com.ceams.module.bpm.mapper.BpmInstanceMapper;
import com.ceams.module.bpm.mapper.BpmTaskMapper;
import com.ceams.module.bpm.support.BpmNotificationPublisher;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** 超时查询单元测试：分片参数校验、按状态+超时时间查询、只读不改状态。 */
@ExtendWith(MockitoExtension.class)
class BpmTimeoutServiceImplTest {

    @Mock private BpmTaskMapper taskMapper;
    @Mock private BpmInstanceMapper instanceMapper;
    @Mock private BpmNotificationPublisher notificationPublisher;

    private BpmProperties properties;
    private BpmTimeoutServiceImpl service;

    @BeforeEach
    void setUp() {
        properties = new BpmProperties();
        service = new BpmTimeoutServiceImpl(taskMapper, instanceMapper, notificationPublisher, properties);
    }

    @Test
    void 分片参数越界时拒绝查询() {
        assertThatThrownBy(() -> service.listOverdueTasks(4, 4, 10))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.VALID_ILLEGAL_ARGUMENT);
    }

    @Test
    void 按状态与超时时间分片查询且不修改任务状态() {
        BpmTask overdue = task(1L);
        Page<BpmTask> page = new Page<>(1, 100, false);
        page.setRecords(List.of(overdue));
        when(taskMapper.selectPage(any(Page.class), any(Wrapper.class))).thenReturn(page);

        List<BpmTask> result = service.listOverdueTasks(0, 4, 100);

        assertThat(result).containsExactly(overdue);
        assertThat(overdue.getStatus()).isEqualTo(BpmTaskStatus.PENDING);
        verify(taskMapper).selectPage(any(Page.class), any(Wrapper.class));
    }

    @Test
    void 未传limit时使用配置批量大小() {
        properties.getTimeout().setBatchSize(50);
        Page<BpmTask> page = new Page<>(1, 50, false);
        when(taskMapper.selectPage(any(Page.class), any(Wrapper.class))).thenReturn(page);

        List<BpmTask> result = service.listOverdueTasks(0, 1, null);

        assertThat(result).isEmpty();
        verify(taskMapper).selectPage(any(Page.class), any(Wrapper.class));
    }

    @Test
    void 扫描提醒时抢占成功才发送通知并回写提醒时间() {
        properties.getTimeout().setShardTotal(1);
        BpmTask overdue = task(1L);
        Page<BpmTask> page = new Page<>(1, 200, false);
        page.setRecords(List.of(overdue));
        when(taskMapper.selectPage(any(Page.class), any(Wrapper.class))).thenReturn(page);
        when(taskMapper.update(any(BpmTask.class), any(Wrapper.class))).thenReturn(1);
        when(instanceMapper.selectBatchIds(any())).thenReturn(List.of(instance(11L)));

        int reminded = service.scanAndRemind();

        assertThat(reminded).isEqualTo(1);
        assertThat(overdue.getRemindedAt()).isNotNull();
        verify(notificationPublisher).onTaskOverdue(any(), eq(overdue));
    }

    @Test
    void 抢占失败时不重复发送提醒() {
        properties.getTimeout().setShardTotal(1);
        BpmTask overdue = task(1L);
        Page<BpmTask> page = new Page<>(1, 200, false);
        page.setRecords(List.of(overdue));
        when(taskMapper.selectPage(any(Page.class), any(Wrapper.class))).thenReturn(page);
        when(taskMapper.update(any(BpmTask.class), any(Wrapper.class))).thenReturn(0);

        int reminded = service.scanAndRemind();

        assertThat(reminded).isZero();
        verify(notificationPublisher, never()).onTaskOverdue(any(), any());
    }

    private BpmTask task(Long id) {
        BpmTask task = new BpmTask();
        task.setId(id);
        task.setInstanceId(11L);
        task.setNodeId(12L);
        task.setAssigneeId(13L);
        task.setStatus(BpmTaskStatus.PENDING);
        task.setDueAt(LocalDateTime.now().minusHours(2));
        return task;
    }

    private BpmInstance instance(Long id) {
        BpmInstance instance = new BpmInstance();
        instance.setId(id);
        instance.setBizType("EXPENSE");
        instance.setBizId(1001L);
        instance.setInitiatorId(1L);
        return instance;
    }
}
