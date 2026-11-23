package com.ceams.module.bpm.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.module.bpm.config.BpmProperties;
import com.ceams.module.bpm.entity.BpmAssigneeSnapshot;
import com.ceams.module.bpm.entity.BpmCcRecord;
import com.ceams.module.bpm.entity.BpmInstance;
import com.ceams.module.bpm.entity.BpmNode;
import com.ceams.module.bpm.entity.BpmTask;
import com.ceams.module.bpm.enums.BpmAssigneeSource;
import com.ceams.module.bpm.enums.BpmInstanceStatus;
import com.ceams.module.bpm.enums.BpmNodeStatus;
import com.ceams.module.bpm.enums.BpmNodeType;
import com.ceams.module.bpm.enums.RejectTarget;
import com.ceams.module.bpm.enums.BpmTaskStatus;
import com.ceams.module.bpm.mapper.BpmAssigneeSnapshotMapper;
import com.ceams.module.bpm.mapper.BpmCcRecordMapper;
import com.ceams.module.bpm.mapper.BpmInstanceMapper;
import com.ceams.module.bpm.mapper.BpmNodeMapper;
import com.ceams.module.bpm.mapper.BpmTaskMapper;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** 流程推进引擎单元测试：覆盖会签/或签的节点推进、驳回回溯、撤回终止与审批人为空。 */
@ExtendWith(MockitoExtension.class)
class BpmFlowEngineTest {

    @Mock private BpmInstanceMapper instanceMapper;
    @Mock private BpmNodeMapper nodeMapper;
    @Mock private BpmTaskMapper taskMapper;
    @Mock private BpmAssigneeSnapshotMapper snapshotMapper;
    @Mock private BpmCcRecordMapper ccRecordMapper;
    @Mock private BpmAssigneeResolver assigneeResolver;
    @Mock private BpmConditionContextBuilder conditionContextBuilder;
    @Mock private BpmNotificationPublisher notificationPublisher;
    @Mock private BpmProxyResolver proxyResolver;

    private BpmProperties properties;
    private BpmFlowEngine engine;

    @BeforeEach
    void setUp() {
        properties = new BpmProperties();
        engine =
                new BpmFlowEngine(
                        instanceMapper,
                        nodeMapper,
                        taskMapper,
                        snapshotMapper,
                        ccRecordMapper,
                        assigneeResolver,
                        properties,
                        new BpmConditionEvaluator(),
                        conditionContextBuilder,
                        notificationPublisher,
                        new BpmBizEventDispatcher(List.of()),
                        proxyResolver);
        lenient().when(instanceMapper.updateById(any(BpmInstance.class))).thenReturn(1);
        lenient().when(nodeMapper.updateById(any(BpmNode.class))).thenReturn(1);
        lenient().when(taskMapper.updateById(any(BpmTask.class))).thenReturn(1);
        // 代理默认不命中（Mockito 对 Long 返回 0，需显式置空）
        lenient().when(proxyResolver.resolve(any(), any(), any())).thenReturn(null);
    }

    @Test
    void 发起时自动跳过开始节点并激活首个审批节点() {
        BpmInstance instance = instance(1L, 10L);
        BpmNode start = node(11L, 1L, "start", BpmNodeType.START, 1, BpmNodeStatus.PENDING);
        BpmNode approve = node(12L, 1L, "approve", BpmNodeType.SINGLE, 2, BpmNodeStatus.PENDING);
        BpmSchema schema = schema(def("start", 1, BpmNodeType.START), approvalDef("approve", 2, BpmNodeType.SINGLE));
        when(assigneeResolver.resolve(any(BpmSchemaNode.class), any(), any(), any()))
                .thenReturn(List.of(resolved(20L, BpmAssigneeSource.USER)));

        engine.start(instance, List.of(start, approve), schema);

        assertThat(start.getStatus()).isEqualTo(BpmNodeStatus.COMPLETED);
        assertThat(approve.getStatus()).isEqualTo(BpmNodeStatus.ACTIVE);
        ArgumentCaptor<BpmTask> captor = ArgumentCaptor.forClass(BpmTask.class);
        verify(taskMapper).insert(captor.capture());
        assertThat(captor.getValue().getAssigneeId()).isEqualTo(20L);
        assertThat(captor.getValue().getStatus()).isEqualTo(BpmTaskStatus.PENDING);
        verify(snapshotMapper).insert(any(BpmAssigneeSnapshot.class));
    }

    @Test
    void 审批人解析为空时任务落待分配且不生成无人待办() {
        BpmInstance instance = instance(1L, 10L);
        BpmNode start = node(11L, 1L, "start", BpmNodeType.START, 1, BpmNodeStatus.PENDING);
        BpmNode approve = node(12L, 1L, "approve", BpmNodeType.COUNTERSIGN, 2, BpmNodeStatus.PENDING);
        BpmSchema schema = schema(def("start", 1, BpmNodeType.START), approvalDef("approve", 2, BpmNodeType.COUNTERSIGN));
        when(assigneeResolver.resolve(any(BpmSchemaNode.class), any(), any(), any())).thenReturn(List.of());

        engine.start(instance, List.of(start, approve), schema);

        ArgumentCaptor<BpmTask> captor = ArgumentCaptor.forClass(BpmTask.class);
        verify(taskMapper).insert(captor.capture());
        assertThat(captor.getValue().getAssigneeId()).isNull();
        assertThat(captor.getValue().getStatus()).isEqualTo(BpmTaskStatus.PENDING_ASSIGNEE);
        verify(snapshotMapper, never()).insert(any(BpmAssigneeSnapshot.class));
    }

    @Test
    void 仅有开始结束节点时实例直接通过() {
        BpmInstance instance = instance(1L, 10L);
        BpmNode start = node(11L, 1L, "start", BpmNodeType.START, 1, BpmNodeStatus.PENDING);
        BpmNode end = node(12L, 1L, "end", BpmNodeType.END, 2, BpmNodeStatus.PENDING);
        BpmSchema schema = schema(def("start", 1, BpmNodeType.START), def("end", 2, BpmNodeType.END));

        engine.start(instance, List.of(start, end), schema);

        assertThat(instance.getStatus()).isEqualTo(BpmInstanceStatus.APPROVED);
        assertThat(start.getStatus()).isEqualTo(BpmNodeStatus.COMPLETED);
        assertThat(end.getStatus()).isEqualTo(BpmNodeStatus.COMPLETED);
    }

    @Test
    void 同意后完成当前节点并激活下一节点() {
        BpmInstance instance = instance(1L, 10L);
        BpmNode current = node(11L, 1L, "first", BpmNodeType.SINGLE, 1, BpmNodeStatus.ACTIVE);
        BpmNode next = node(12L, 1L, "second", BpmNodeType.SINGLE, 2, BpmNodeStatus.PENDING);
        BpmSchema schema = schema(approvalDef("first", 1, BpmNodeType.SINGLE), approvalDef("second", 2, BpmNodeType.SINGLE));
        when(assigneeResolver.resolve(any(BpmSchemaNode.class), any(), any(), any()))
                .thenReturn(List.of(resolved(21L, BpmAssigneeSource.USER)));

        engine.afterApprove(instance, List.of(current, next), current, schema);

        assertThat(current.getStatus()).isEqualTo(BpmNodeStatus.COMPLETED);
        assertThat(next.getStatus()).isEqualTo(BpmNodeStatus.ACTIVE);
        assertThat(instance.getStatus()).isEqualTo(BpmInstanceStatus.RUNNING);
    }

    @Test
    void 最后节点同意后实例通过() {
        BpmInstance instance = instance(1L, 10L);
        BpmNode current = node(11L, 1L, "only", BpmNodeType.SINGLE, 1, BpmNodeStatus.ACTIVE);
        BpmSchema schema = schema(approvalDef("only", 1, BpmNodeType.SINGLE));

        engine.afterApprove(instance, List.of(current), current, schema);

        assertThat(instance.getStatus()).isEqualTo(BpmInstanceStatus.APPROVED);
        assertThat(current.getStatus()).isEqualTo(BpmNodeStatus.COMPLETED);
        assertThat(instance.getEndedAt()).isNotNull();
    }

    @Test
    void 驳回上一级复用快照并重新激活上一节点() {
        BpmInstance instance = instance(1L, 10L);
        BpmNode previous = node(11L, 1L, "first", BpmNodeType.SINGLE, 1, BpmNodeStatus.COMPLETED);
        BpmNode current = node(12L, 1L, "second", BpmNodeType.SINGLE, 2, BpmNodeStatus.ACTIVE);
        BpmSchema schema = schema(approvalDef("first", 1, BpmNodeType.SINGLE), approvalDef("second", 2, BpmNodeType.SINGLE));
        BpmAssigneeSnapshot snapshot = new BpmAssigneeSnapshot();
        snapshot.setNodeId(11L);
        snapshot.setAssigneeId(30L);
        snapshot.setSourceType(BpmAssigneeSource.USER);
        when(snapshotMapper.selectList(any(Wrapper.class))).thenReturn(List.of(snapshot));

        engine.afterReject(instance, List.of(previous, current), current, schema, RejectTarget.PREVIOUS);

        assertThat(current.getStatus()).isEqualTo(BpmNodeStatus.REJECTED);
        assertThat(previous.getStatus()).isEqualTo(BpmNodeStatus.ACTIVE);
        assertThat(previous.getCompletedAt()).isNull();
        ArgumentCaptor<BpmTask> captor = ArgumentCaptor.forClass(BpmTask.class);
        verify(taskMapper).insert(captor.capture());
        assertThat(captor.getValue().getAssigneeId()).isEqualTo(30L);
        assertThat(instance.getStatus()).isEqualTo(BpmInstanceStatus.RUNNING);
    }

    @Test
    void 驳回发起人时首节点重新激活并生成发起人待办() {
        BpmInstance instance = instance(1L, 10L);
        BpmNode first = node(11L, 1L, "start", BpmNodeType.START, 1, BpmNodeStatus.COMPLETED);
        BpmNode current = node(12L, 1L, "approve", BpmNodeType.SINGLE, 2, BpmNodeStatus.ACTIVE);
        BpmSchema schema = schema(def("start", 1, BpmNodeType.START), approvalDef("approve", 2, BpmNodeType.SINGLE));

        engine.afterReject(instance, List.of(first, current), current, schema, RejectTarget.INITIATOR);

        assertThat(first.getStatus()).isEqualTo(BpmNodeStatus.ACTIVE);
        ArgumentCaptor<BpmTask> captor = ArgumentCaptor.forClass(BpmTask.class);
        verify(taskMapper).insert(captor.capture());
        assertThat(captor.getValue().getAssigneeId()).isEqualTo(10L);
        assertThat(captor.getValue().getSourceType()).isEqualTo(BpmAssigneeSource.INITIATOR);
    }

    @Test
    void 首节点驳回时实例驳回终止() {
        BpmInstance instance = instance(1L, 10L);
        BpmNode current = node(11L, 1L, "only", BpmNodeType.SINGLE, 1, BpmNodeStatus.ACTIVE);
        BpmSchema schema = schema(approvalDef("only", 1, BpmNodeType.SINGLE));
        when(taskMapper.selectList(any(Wrapper.class))).thenReturn(List.of());

        engine.afterReject(instance, List.of(current), current, schema, RejectTarget.INITIATOR);

        assertThat(instance.getStatus()).isEqualTo(BpmInstanceStatus.REJECTED);
        assertThat(current.getStatus()).isEqualTo(BpmNodeStatus.REJECTED);
    }

    @Test
    void 撤回关闭在途任务并将实例置撤回() {
        BpmInstance instance = instance(1L, 10L);
        BpmNode current = node(11L, 1L, "only", BpmNodeType.SINGLE, 1, BpmNodeStatus.ACTIVE);
        BpmTask pending = task(21L, 1L, 11L, 20L, BpmTaskStatus.PENDING);
        when(taskMapper.selectList(any(Wrapper.class))).thenReturn(List.of(pending));

        engine.withdraw(instance, List.of(current));

        assertThat(instance.getStatus()).isEqualTo(BpmInstanceStatus.WITHDRAWN);
        assertThat(pending.getStatus()).isEqualTo(BpmTaskStatus.CLOSED);
        assertThat(current.getStatus()).isEqualTo(BpmNodeStatus.CLOSED);
    }

    @Test
    void 终止关闭在途任务并将实例置终止() {
        BpmInstance instance = instance(1L, 10L);
        BpmNode current = node(11L, 1L, "only", BpmNodeType.SINGLE, 1, BpmNodeStatus.ACTIVE);
        when(taskMapper.selectList(any(Wrapper.class))).thenReturn(List.of());

        engine.terminate(instance, List.of(current));

        assertThat(instance.getStatus()).isEqualTo(BpmInstanceStatus.TERMINATED);
    }

    @Test
    void 抄送节点写入抄送记录并自动跳过() {
        BpmInstance instance = instance(1L, 10L);
        BpmNode start = node(11L, 1L, "start", BpmNodeType.START, 1, BpmNodeStatus.PENDING);
        BpmNode cc = node(12L, 1L, "cc", BpmNodeType.CC, 2, BpmNodeStatus.PENDING);
        BpmNode approve = node(13L, 1L, "approve", BpmNodeType.SINGLE, 3, BpmNodeStatus.PENDING);
        BpmSchema schema =
                schema(
                        def("start", 1, BpmNodeType.START),
                        approvalDef("cc", 2, BpmNodeType.CC),
                        approvalDef("approve", 3, BpmNodeType.SINGLE));
        when(assigneeResolver.resolve(any(BpmSchemaNode.class), any(), any(), any()))
                .thenReturn(List.of(resolved(40L, BpmAssigneeSource.ROLE)));

        engine.start(instance, List.of(start, cc, approve), schema);

        verify(ccRecordMapper).insert(any(BpmCcRecord.class));
        assertThat(cc.getStatus()).isEqualTo(BpmNodeStatus.COMPLETED);
        assertThat(approve.getStatus()).isEqualTo(BpmNodeStatus.ACTIVE);
    }

    @Test
    void 已处理任务判定用于撤回前置校验() {
        when(taskMapper.selectCount(any(Wrapper.class))).thenReturn(1L);
        assertThat(engine.hasProcessedTask(1L)).isTrue();
    }

    @Test
    void 条件节点金额达到阈值时走命中分支() {
        BpmInstance instance = instance(1L, 10L);
        BpmNode start = node(11L, 1L, "start", BpmNodeType.START, 1, BpmNodeStatus.PENDING);
        BpmNode cond = node(12L, 1L, "cond", BpmNodeType.CONDITION, 2, BpmNodeStatus.PENDING);
        BpmNode high = node(13L, 1L, "high", BpmNodeType.SINGLE, 3, BpmNodeStatus.PENDING);
        BpmNode low = node(14L, 1L, "low", BpmNodeType.SINGLE, 4, BpmNodeStatus.PENDING);
        BpmNode end = node(15L, 1L, "end", BpmNodeType.END, 5, BpmNodeStatus.PENDING);
        BpmSchemaNode condDef = def("cond", 2, BpmNodeType.CONDITION);
        condDef.setDefaultNext("low");
        condDef.setConditions(List.of(condition("amount", "GE", "5000", "high")));
        BpmSchema schema =
                schema(
                        def("start", 1, BpmNodeType.START),
                        condDef,
                        approvalDef("high", 3, BpmNodeType.SINGLE),
                        approvalDef("low", 4, BpmNodeType.SINGLE),
                        def("end", 5, BpmNodeType.END));
        when(conditionContextBuilder.build(instance))
                .thenReturn(context(Map.of("amount", "8000"), Set.of()));
        when(assigneeResolver.resolve(any(BpmSchemaNode.class), any(), any(), any()))
                .thenReturn(List.of(resolved(20L, BpmAssigneeSource.USER)));

        engine.start(instance, List.of(start, cond, high, low, end), schema);

        assertThat(cond.getStatus()).isEqualTo(BpmNodeStatus.COMPLETED);
        assertThat(high.getStatus()).isEqualTo(BpmNodeStatus.ACTIVE);
        assertThat(low.getStatus()).isEqualTo(BpmNodeStatus.PENDING);
    }

    @Test
    void 条件节点金额未命中时走默认分支() {
        BpmInstance instance = instance(1L, 10L);
        BpmNode start = node(11L, 1L, "start", BpmNodeType.START, 1, BpmNodeStatus.PENDING);
        BpmNode cond = node(12L, 1L, "cond", BpmNodeType.CONDITION, 2, BpmNodeStatus.PENDING);
        BpmNode high = node(13L, 1L, "high", BpmNodeType.SINGLE, 3, BpmNodeStatus.PENDING);
        BpmNode low = node(14L, 1L, "low", BpmNodeType.SINGLE, 4, BpmNodeStatus.PENDING);
        BpmNode end = node(15L, 1L, "end", BpmNodeType.END, 5, BpmNodeStatus.PENDING);
        BpmSchemaNode condDef = def("cond", 2, BpmNodeType.CONDITION);
        condDef.setDefaultNext("low");
        condDef.setConditions(List.of(condition("amount", "GE", "5000", "high")));
        BpmSchema schema =
                schema(
                        def("start", 1, BpmNodeType.START),
                        condDef,
                        approvalDef("high", 3, BpmNodeType.SINGLE),
                        approvalDef("low", 4, BpmNodeType.SINGLE),
                        def("end", 5, BpmNodeType.END));
        when(conditionContextBuilder.build(instance))
                .thenReturn(context(Map.of("amount", "2000"), Set.of()));
        when(assigneeResolver.resolve(any(BpmSchemaNode.class), any(), any(), any()))
                .thenReturn(List.of(resolved(20L, BpmAssigneeSource.USER)));

        engine.start(instance, List.of(start, cond, high, low, end), schema);

        assertThat(low.getStatus()).isEqualTo(BpmNodeStatus.ACTIVE);
        assertThat(high.getStatus()).isEqualTo(BpmNodeStatus.PENDING);
    }

    @Test
    void 条件节点角色命中时走命中分支() {
        BpmInstance instance = instance(1L, 10L);
        BpmNode start = node(11L, 1L, "start", BpmNodeType.START, 1, BpmNodeStatus.PENDING);
        BpmNode cond = node(12L, 1L, "cond", BpmNodeType.CONDITION, 2, BpmNodeStatus.PENDING);
        BpmNode finance = node(13L, 1L, "finance", BpmNodeType.SINGLE, 3, BpmNodeStatus.PENDING);
        BpmNode normal = node(14L, 1L, "normal", BpmNodeType.SINGLE, 4, BpmNodeStatus.PENDING);
        BpmNode end = node(15L, 1L, "end", BpmNodeType.END, 5, BpmNodeStatus.PENDING);
        BpmSchemaNode condDef = def("cond", 2, BpmNodeType.CONDITION);
        condDef.setDefaultNext("normal");
        condDef.setConditions(List.of(condition("initiatorRole", "EQ", "FINANCE", "finance")));
        BpmSchema schema =
                schema(
                        def("start", 1, BpmNodeType.START),
                        condDef,
                        approvalDef("finance", 3, BpmNodeType.SINGLE),
                        approvalDef("normal", 4, BpmNodeType.SINGLE),
                        def("end", 5, BpmNodeType.END));
        when(conditionContextBuilder.build(instance))
                .thenReturn(context(Map.of(), Set.of("FINANCE")));
        when(assigneeResolver.resolve(any(BpmSchemaNode.class), any(), any(), any()))
                .thenReturn(List.of(resolved(20L, BpmAssigneeSource.USER)));

        engine.start(instance, List.of(start, cond, finance, normal, end), schema);

        assertThat(finance.getStatus()).isEqualTo(BpmNodeStatus.ACTIVE);
        assertThat(normal.getStatus()).isEqualTo(BpmNodeStatus.PENDING);
    }

    @Test
    void 条件节点按配置顺序取第一个命中分支() {
        BpmInstance instance = instance(1L, 10L);
        BpmNode start = node(11L, 1L, "start", BpmNodeType.START, 1, BpmNodeStatus.PENDING);
        BpmNode cond = node(12L, 1L, "cond", BpmNodeType.CONDITION, 2, BpmNodeStatus.PENDING);
        BpmNode high = node(13L, 1L, "high", BpmNodeType.SINGLE, 3, BpmNodeStatus.PENDING);
        BpmNode mid = node(14L, 1L, "mid", BpmNodeType.SINGLE, 4, BpmNodeStatus.PENDING);
        BpmNode low = node(15L, 1L, "low", BpmNodeType.SINGLE, 5, BpmNodeStatus.PENDING);
        BpmNode end = node(16L, 1L, "end", BpmNodeType.END, 6, BpmNodeStatus.PENDING);
        BpmSchemaNode condDef = def("cond", 2, BpmNodeType.CONDITION);
        condDef.setDefaultNext("low");
        condDef.setConditions(
                List.of(
                        condition("amount", "GE", "10000", "high"),
                        condition("amount", "GE", "5000", "mid")));
        BpmSchema schema =
                schema(
                        def("start", 1, BpmNodeType.START),
                        condDef,
                        approvalDef("high", 3, BpmNodeType.SINGLE),
                        approvalDef("mid", 4, BpmNodeType.SINGLE),
                        approvalDef("low", 5, BpmNodeType.SINGLE),
                        def("end", 6, BpmNodeType.END));
        when(conditionContextBuilder.build(instance))
                .thenReturn(context(Map.of("amount", "8000"), Set.of()));
        when(assigneeResolver.resolve(any(BpmSchemaNode.class), any(), any(), any()))
                .thenReturn(List.of(resolved(20L, BpmAssigneeSource.USER)));

        engine.start(instance, List.of(start, cond, high, mid, low, end), schema);

        assertThat(mid.getStatus()).isEqualTo(BpmNodeStatus.ACTIVE);
        assertThat(high.getStatus()).isEqualTo(BpmNodeStatus.PENDING);
        assertThat(low.getStatus()).isEqualTo(BpmNodeStatus.PENDING);
    }

    @Test
    void 条件节点无默认出口时抛出模板错误() {
        BpmInstance instance = instance(1L, 10L);
        BpmNode start = node(11L, 1L, "start", BpmNodeType.START, 1, BpmNodeStatus.PENDING);
        BpmNode cond = node(12L, 1L, "cond", BpmNodeType.CONDITION, 2, BpmNodeStatus.PENDING);
        BpmNode high = node(13L, 1L, "high", BpmNodeType.SINGLE, 3, BpmNodeStatus.PENDING);
        BpmNode end = node(14L, 1L, "end", BpmNodeType.END, 4, BpmNodeStatus.PENDING);
        BpmSchemaNode condDef = def("cond", 2, BpmNodeType.CONDITION);
        condDef.setConditions(List.of(condition("amount", "GE", "5000", "high")));
        BpmSchema schema =
                schema(
                        def("start", 1, BpmNodeType.START),
                        condDef,
                        approvalDef("high", 3, BpmNodeType.SINGLE),
                        def("end", 4, BpmNodeType.END));
        when(conditionContextBuilder.build(instance))
                .thenReturn(context(Map.of("amount", "100"), Set.of()));

        assertThatThrownBy(() -> engine.start(instance, List.of(start, cond, high, end), schema))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.BPM_SCHEMA_INVALID);
    }

    @Test
    void 普通节点优先按显式nextNodeKey路由() {
        BpmInstance instance = instance(1L, 10L);
        BpmNode start = node(11L, 1L, "start", BpmNodeType.START, 1, BpmNodeStatus.PENDING);
        BpmNode second = node(12L, 1L, "second", BpmNodeType.SINGLE, 2, BpmNodeStatus.PENDING);
        BpmNode third = node(13L, 1L, "third", BpmNodeType.SINGLE, 3, BpmNodeStatus.PENDING);
        BpmNode end = node(14L, 1L, "end", BpmNodeType.END, 4, BpmNodeStatus.PENDING);
        BpmSchemaNode startDef = def("start", 1, BpmNodeType.START);
        startDef.setNextNodeKey("third");
        BpmSchema schema =
                schema(
                        startDef,
                        approvalDef("second", 2, BpmNodeType.SINGLE),
                        approvalDef("third", 3, BpmNodeType.SINGLE),
                        def("end", 4, BpmNodeType.END));
        when(assigneeResolver.resolve(any(BpmSchemaNode.class), any(), any(), any()))
                .thenReturn(List.of(resolved(20L, BpmAssigneeSource.USER)));

        engine.start(instance, List.of(start, second, third, end), schema);

        assertThat(third.getStatus()).isEqualTo(BpmNodeStatus.ACTIVE);
        assertThat(second.getStatus()).isEqualTo(BpmNodeStatus.PENDING);
    }

    @Test
    void 审批人仅含发起人时节点自动通过() {
        BpmInstance instance = instance(1L, 10L);
        BpmNode start = node(11L, 1L, "start", BpmNodeType.START, 1, BpmNodeStatus.PENDING);
        BpmNode approve = node(12L, 1L, "approve", BpmNodeType.SINGLE, 2, BpmNodeStatus.PENDING);
        BpmNode end = node(13L, 1L, "end", BpmNodeType.END, 3, BpmNodeStatus.PENDING);
        BpmSchemaNode approveDef = approvalDef("approve", 2, BpmNodeType.SINGLE);
        approveDef.setAutoApproveIfInitiator(Boolean.TRUE);
        BpmSchema schema =
                schema(
                        def("start", 1, BpmNodeType.START),
                        approveDef,
                        def("end", 3, BpmNodeType.END));
        when(assigneeResolver.resolve(any(BpmSchemaNode.class), any(), any(), any()))
                .thenReturn(List.of(resolved(10L, BpmAssigneeSource.INITIATOR)));

        engine.start(instance, List.of(start, approve, end), schema);

        assertThat(approve.getStatus()).isEqualTo(BpmNodeStatus.COMPLETED);
        assertThat(instance.getStatus()).isEqualTo(BpmInstanceStatus.APPROVED);
        ArgumentCaptor<BpmTask> captor = ArgumentCaptor.forClass(BpmTask.class);
        verify(taskMapper).insert(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(BpmTaskStatus.APPROVED);
        assertThat(captor.getValue().getAssigneeId()).isEqualTo(10L);
    }

    @Test
    void 自动通过条件命中时节点自动通过() {
        BpmInstance instance = instance(1L, 10L);
        BpmNode start = node(11L, 1L, "start", BpmNodeType.START, 1, BpmNodeStatus.PENDING);
        BpmNode approve = node(12L, 1L, "approve", BpmNodeType.SINGLE, 2, BpmNodeStatus.PENDING);
        BpmNode end = node(13L, 1L, "end", BpmNodeType.END, 3, BpmNodeStatus.PENDING);
        BpmSchemaNode approveDef = approvalDef("approve", 2, BpmNodeType.SINGLE);
        approveDef.setAutoApproveConditions(List.of(condition("amount", "GE", "8000", null)));
        BpmSchema schema =
                schema(
                        def("start", 1, BpmNodeType.START),
                        approveDef,
                        def("end", 3, BpmNodeType.END));
        when(conditionContextBuilder.build(instance))
                .thenReturn(context(Map.of("amount", "9000"), Set.of()));
        when(assigneeResolver.resolve(any(BpmSchemaNode.class), any(), any(), any()))
                .thenReturn(List.of(resolved(20L, BpmAssigneeSource.USER)));

        engine.start(instance, List.of(start, approve, end), schema);

        assertThat(approve.getStatus()).isEqualTo(BpmNodeStatus.COMPLETED);
        assertThat(instance.getStatus()).isEqualTo(BpmInstanceStatus.APPROVED);
    }

    @Test
    void 审批人处于代理时段时待办转交代理人() {
        BpmInstance instance = instance(1L, 10L);
        BpmNode start = node(11L, 1L, "start", BpmNodeType.START, 1, BpmNodeStatus.PENDING);
        BpmNode approve = node(12L, 1L, "approve", BpmNodeType.SINGLE, 2, BpmNodeStatus.PENDING);
        BpmSchema schema = schema(def("start", 1, BpmNodeType.START), approvalDef("approve", 2, BpmNodeType.SINGLE));
        when(assigneeResolver.resolve(any(BpmSchemaNode.class), any(), any(), any()))
                .thenReturn(List.of(resolved(20L, BpmAssigneeSource.USER)));
        when(proxyResolver.resolve(any(), any(), any())).thenReturn(99L);

        engine.start(instance, List.of(start, approve), schema);

        ArgumentCaptor<BpmTask> captor = ArgumentCaptor.forClass(BpmTask.class);
        verify(taskMapper).insert(captor.capture());
        assertThat(captor.getValue().getAssigneeId()).isEqualTo(99L);
        assertThat(captor.getValue().getOwnerAssigneeId()).isEqualTo(20L);
        assertThat(captor.getValue().getSourceType()).isEqualTo(BpmAssigneeSource.PROXY);
    }

    // ------------------------------------------------------------------
    // 测试夹具
    // ------------------------------------------------------------------

    private BpmConditionContext context(Map<String, String> variables, Set<String> roles) {
        BpmConditionContext context = new BpmConditionContext();
        context.setVariables(variables);
        context.setRoles(roles);
        return context;
    }

    private BpmSchemaCondition condition(String field, String operator, String value, String target) {
        BpmSchemaCondition condition = new BpmSchemaCondition();
        condition.setField(field);
        condition.setOperator(operator);
        condition.setValue(value);
        condition.setTargetNodeKey(target);
        return condition;
    }

    private BpmInstance instance(Long id, Long initiatorId) {
        BpmInstance instance = new BpmInstance();
        instance.setId(id);
        instance.setInitiatorId(initiatorId);
        instance.setStatus(BpmInstanceStatus.RUNNING);
        instance.setVersion(1);
        return instance;
    }

    private BpmNode node(
            Long id, Long instanceId, String key, BpmNodeType type, int seq, BpmNodeStatus status) {
        BpmNode node = new BpmNode();
        node.setId(id);
        node.setInstanceId(instanceId);
        node.setNodeKey(key);
        node.setNodeType(type);
        node.setSeqNo(seq);
        node.setStatus(status);
        node.setVersion(0);
        return node;
    }

    private BpmTask task(Long id, Long instanceId, Long nodeId, Long assignee, BpmTaskStatus status) {
        BpmTask task = new BpmTask();
        task.setId(id);
        task.setInstanceId(instanceId);
        task.setNodeId(nodeId);
        task.setAssigneeId(assignee);
        task.setStatus(status);
        task.setVersion(0);
        return task;
    }

    private BpmSchema schema(BpmSchemaNode... nodes) {
        BpmSchema schema = new BpmSchema();
        schema.setNodes(List.of(nodes));
        return schema;
    }

    private BpmSchemaNode def(String key, int seq, BpmNodeType type) {
        BpmSchemaNode node = new BpmSchemaNode();
        node.setNodeKey(key);
        node.setSeqNo(seq);
        node.setNodeType(type);
        return node;
    }

    private BpmSchemaNode approvalDef(String key, int seq, BpmNodeType type) {
        BpmSchemaNode node = def(key, seq, type);
        BpmAssigneeSourceConfig source = new BpmAssigneeSourceConfig();
        source.setType(BpmAssigneeSource.USER);
        source.setValue("20");
        node.setAssignees(List.of(source));
        return node;
    }

    private BpmResolvedAssignee resolved(Long id, BpmAssigneeSource type) {
        BpmResolvedAssignee assignee = new BpmResolvedAssignee();
        assignee.setAssigneeId(id);
        assignee.setSourceType(type);
        return assignee;
    }
}
