package com.ceams.module.bpm.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.module.bpm.config.BpmProperties;
import com.ceams.module.bpm.entity.BpmAssigneeSnapshot;
import com.ceams.module.bpm.entity.BpmCcRecord;
import com.ceams.module.bpm.entity.BpmInstance;
import com.ceams.module.bpm.entity.BpmNode;
import com.ceams.module.bpm.entity.BpmTask;
import com.ceams.module.bpm.enums.BpmAssigneeSource;
import com.ceams.module.bpm.enums.BpmBizType;
import com.ceams.module.bpm.enums.CcReadStatus;
import com.ceams.module.bpm.enums.BpmInstanceStatus;
import com.ceams.module.bpm.enums.BpmNodeStatus;
import com.ceams.module.bpm.enums.BpmNodeType;
import com.ceams.module.bpm.enums.RejectTarget;
import com.ceams.module.bpm.enums.BpmTaskStatus;
import com.ceams.module.bpm.enums.TaskType;
import com.ceams.module.bpm.mapper.BpmAssigneeSnapshotMapper;
import com.ceams.module.bpm.mapper.BpmCcRecordMapper;
import com.ceams.module.bpm.mapper.BpmInstanceMapper;
import com.ceams.module.bpm.mapper.BpmNodeMapper;
import com.ceams.module.bpm.mapper.BpmTaskMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * BPM 流程推进引擎：负责节点到达、自动跳过、审批推进、驳回回溯与实例收口。
 *
 * <p>并发约定：所有节点/实例更新均使用 {@code version} 乐观锁；若更新失败但目标状态
 * 已由并发请求达成，按幂等处理返回，否则抛出 {@link ErrorCode#BPM_INSTANCE_CONFLICT}。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BpmFlowEngine {

    private final BpmInstanceMapper instanceMapper;
    private final BpmNodeMapper nodeMapper;
    private final BpmTaskMapper taskMapper;
    private final BpmAssigneeSnapshotMapper snapshotMapper;
    private final BpmCcRecordMapper ccRecordMapper;
    private final BpmAssigneeResolver assigneeResolver;
    private final BpmProperties bpmProperties;
    private final BpmConditionEvaluator conditionEvaluator;
    private final BpmConditionContextBuilder conditionContextBuilder;
    private final BpmNotificationPublisher notificationPublisher;
    private final BpmBizEventDispatcher bizEventDispatcher;
    private final BpmProxyResolver proxyResolver;

    /** 发起：从首节点开始推进，自动跳过 START/CC/CONDITION/AUTO/END 虚拟节点。 */
    @Transactional(rollbackFor = Exception.class)
    public void start(BpmInstance instance, List<BpmNode> nodes, BpmSchema schema) {
        advance(instance, nodes, schema, null);
    }

    /**
     * 锁定节点行（SELECT ... FOR UPDATE），用于串行化同一节点的并发审批。
     *
     * <p>必须在调用方事务内执行；加锁后再重新读取任务与统计待办数量，才能保证
     * 会签/或签最后一人准确推进节点。
     */
    public void lockNode(Long nodeId) {
        nodeMapper.selectOne(
                new LambdaQueryWrapper<BpmNode>()
                        .eq(BpmNode::getId, nodeId)
                        .last("FOR UPDATE"));
    }

    /** 同意后推进：先完成当前节点，再激活下一节点或收口实例。 */
    @Transactional(rollbackFor = Exception.class)
    public void afterApprove(
            BpmInstance instance, List<BpmNode> nodes, BpmNode currentNode, BpmSchema schema) {
        currentNode.setStatus(BpmNodeStatus.COMPLETED);
        currentNode.setCompletedAt(LocalDateTime.now());
        if (!persistNode(currentNode)) {
            BpmNode latest = nodeMapper.selectById(currentNode.getId());
            if (latest != null && latest.getStatus() == BpmNodeStatus.COMPLETED) {
                // 并发场景下其他请求已完成该节点，按幂等成功返回
                return;
            }
            throw BusinessException.of(ErrorCode.BPM_INSTANCE_CONFLICT, "节点已被其他操作更新，请刷新重试");
        }
        // 节点完成事件：业务侧据此写回节点级结果（如财务核验通过）
        bizEventDispatcher.onNodeCompleted(instance, currentNode.getNodeKey());
        advance(instance, nodes, schema, currentNode);
    }

    /**
     * 驳回：关闭当前节点，按目标回溯到首节点/上一节点；无可用目标时实例驳回终止。
     *
     * <p>回溯到上一级时优先复用 bpm_assignee_snapshot，保持审批人稳定不漂移。
     */
    @Transactional(rollbackFor = Exception.class)
    public void afterReject(
            BpmInstance instance,
            List<BpmNode> nodes,
            BpmNode currentNode,
            BpmSchema schema,
            RejectTarget target) {
        closeTasks(
                taskMapper.selectList(
                        new LambdaQueryWrapper<BpmTask>()
                                .eq(BpmTask::getNodeId, currentNode.getId())
                                .eq(BpmTask::getStatus, BpmTaskStatus.PENDING)));
        currentNode.setStatus(BpmNodeStatus.REJECTED);
        currentNode.setCompletedAt(LocalDateTime.now());
        persistNodeOrThrow(currentNode);

        BpmNode targetNode = resolveRejectNode(instance, nodes, currentNode, target);
        if (targetNode == null || Objects.equals(targetNode.getId(), currentNode.getId())) {
            // 无可回溯节点：实例驳回终止，关闭全部在途任务
            finish(instance, nodes, BpmInstanceStatus.REJECTED);
            return;
        }

        targetNode.setStatus(BpmNodeStatus.ACTIVE);
        targetNode.setArrivedAt(LocalDateTime.now());
        targetNode.setCompletedAt(null);
        persistNodeOrThrow(targetNode);

        if (target == RejectTarget.INITIATOR) {
            activateWithAssignees(
                    instance,
                    targetNode,
                    List.of(initiatorAssignee(instance)),
                    bpmProperties.getDefaultDueHours());
        } else {
            List<BpmAssigneeSnapshot> snapshots =
                    snapshotMapper.selectList(
                            new LambdaQueryWrapper<BpmAssigneeSnapshot>()
                                    .eq(BpmAssigneeSnapshot::getNodeId, targetNode.getId()));
            List<BpmResolvedAssignee> resolved =
                    snapshots.stream()
                            .map(
                                    s -> {
                                        BpmResolvedAssignee a = new BpmResolvedAssignee();
                                        a.setAssigneeId(s.getAssigneeId());
                                        a.setSourceType(s.getSourceType());
                                        return a;
                                    })
                            .toList();
            if (resolved.isEmpty()) {
                BpmSchemaNode def = schema.findByKey(targetNode.getNodeKey());
                resolved =
                        assigneeResolver.resolve(
                                def,
                                instance.getInitiatorId(),
                                BpmBizType.from(instance.getBizType()),
                                instance.getBizId());
            }
            activateWithAssignees(
                    instance, targetNode, resolved, resolveDueHours(schema, targetNode));
        }
        touchInstance(instance);
    }

    /** 撤回：实例置 WITHDRAWN，关闭全部在途任务与未完成节点。 */
    @Transactional(rollbackFor = Exception.class)
    public void withdraw(BpmInstance instance, List<BpmNode> nodes) {
        finish(instance, nodes, BpmInstanceStatus.WITHDRAWN);
    }

    /** 终止：实例置 TERMINATED，关闭全部在途任务与未完成节点。 */
    @Transactional(rollbackFor = Exception.class)
    public void terminate(BpmInstance instance, List<BpmNode> nodes) {
        finish(instance, nodes, BpmInstanceStatus.TERMINATED);
    }

    /** 改派后补齐节点快照（新任务沿用原节点）。 */
    public void recordDelegateSnapshot(BpmNode node, Long assigneeId) {
        BpmAssigneeSnapshot snapshot = new BpmAssigneeSnapshot();
        snapshot.setNodeId(node.getId());
        snapshot.setAssigneeId(assigneeId);
        snapshot.setSourceType(BpmAssigneeSource.DELEGATE);
        snapshot.setResolvedAt(LocalDateTime.now());
        snapshotMapper.insert(snapshot);
    }

    /** 计算任务超时时间。 */
    public LocalDateTime calculateDueAt(BpmSchema schema, BpmNode node) {
        return LocalDateTime.now().plusHours(resolveDueHours(schema, node));
    }

    // ------------------------------------------------------------------
    // 内部实现
    // ------------------------------------------------------------------

    /**
     * 从指定节点之后推进：普通节点优先按 schema 的 nextNodeKey 路由，未配置时按 seqNo 回退；
     * 条件节点按 conditions 顺序求值，命中取 targetNodeKey，全部不命中走 defaultNext。
     * 虚拟节点（START/CC/CONDITION/AUTO/END）自动完成，审批节点到达后停止。
     *
     * @param from 当前已完成节点；发起时为 null（从首节点开始）
     */
    private void advance(
            BpmInstance instance, List<BpmNode> nodes, BpmSchema schema, BpmNode from) {
        BpmConditionContext conditionContext = null;
        BpmNode cursor = from;
        while (true) {
            BpmSchemaNode cursorDef = cursor == null ? null : schema.findByKey(cursor.getNodeKey());
            BpmNode next;
            if (cursorDef != null && cursorDef.getNodeType() == BpmNodeType.CONDITION) {
                if (conditionContext == null) {
                    conditionContext = conditionContextBuilder.build(instance);
                }
                String targetKey = routeCondition(cursorDef, conditionContext);
                next = routeTarget(nodes, cursor, targetKey);
            } else {
                next = resolveNext(nodes, schema, cursor);
            }
            if (next == null) {
                finishInstanceOnly(instance, BpmInstanceStatus.APPROVED);
                return;
            }
            BpmSchemaNode def = schema.findByKey(next.getNodeKey());
            if (def == null) {
                throw BusinessException.of(ErrorCode.BPM_SCHEMA_INVALID, "节点定义缺失：" + next.getNodeKey());
            }
            if (next.getNodeType() == BpmNodeType.START) {
                autoComplete(next);
                cursor = next;
                continue;
            }
            if (next.getNodeType() == BpmNodeType.CC) {
                next.setArrivedAt(LocalDateTime.now());
                writeCcRecords(instance, next, def);
                autoComplete(next);
                cursor = next;
                continue;
            }
            if (next.getNodeType() == BpmNodeType.CONDITION || next.getNodeType() == BpmNodeType.AUTO) {
                // 条件节点下一轮按条件路由；自动节点直接完成
                autoComplete(next);
                cursor = next;
                continue;
            }
            if (next.getNodeType() == BpmNodeType.END) {
                autoComplete(next);
                finishInstanceOnly(instance, BpmInstanceStatus.APPROVED);
                return;
            }
            List<BpmResolvedAssignee> resolved =
                    assigneeResolver.resolve(
                            def,
                            instance.getInitiatorId(),
                            BpmBizType.from(instance.getBizType()),
                            instance.getBizId());
            if (shouldAutoApprove(instance, def, resolved)) {
                autoApprove(instance, next, resolved);
                cursor = next;
                continue;
            }
            activate(instance, next, def, resolved);
            return;
        }
    }

    /** 普通节点路由：显式 nextNodeKey 优先（目标须仍待到达），否则按顺序号回退。 */
    private BpmNode resolveNext(List<BpmNode> nodes, BpmSchema schema, BpmNode cursor) {
        if (cursor == null) {
            return firstPending(nodes);
        }
        BpmSchemaNode def = schema == null ? null : schema.findByKey(cursor.getNodeKey());
        if (def != null && StringUtils.hasText(def.getNextNodeKey())) {
            BpmNode target = findNode(nodes, def.getNextNodeKey());
            if (target != null && target.getStatus() == BpmNodeStatus.PENDING) {
                return target;
            }
        }
        return nextPendingAfter(nodes, cursor.getSeqNo());
    }

    /** 条件节点路由：按配置顺序取第一个命中条件，全部不命中走默认出口。 */
    private String routeCondition(BpmSchemaNode def, BpmConditionContext context) {
        for (BpmSchemaCondition condition : def.getConditions()) {
            if (conditionEvaluator.matches(condition, context)) {
                return condition.getTargetNodeKey();
            }
        }
        if (StringUtils.hasText(def.getDefaultNext())) {
            return def.getDefaultNext();
        }
        if (StringUtils.hasText(def.getNextNodeKey())) {
            return def.getNextNodeKey();
        }
        throw BusinessException.of(
                ErrorCode.BPM_SCHEMA_INVALID, "条件节点 " + def.getNodeKey() + " 无可用的默认出口");
    }

    /**
     * 条件命中目标定位：目标须仍待到达；若已非待到达（驳回回流等场景），
     * 回退到顺序号之后的第一个待到达节点，保持对现有模板兼容。
     */
    private BpmNode routeTarget(List<BpmNode> nodes, BpmNode cursor, String targetKey) {
        BpmNode target = findNode(nodes, targetKey);
        if (target != null && target.getStatus() == BpmNodeStatus.PENDING) {
            return target;
        }
        return nextPendingAfter(nodes, cursor.getSeqNo());
    }

    private BpmNode findNode(List<BpmNode> nodes, String nodeKey) {
        if (!StringUtils.hasText(nodeKey)) {
            return null;
        }
        return nodes.stream().filter(n -> nodeKey.equals(n.getNodeKey())).findFirst().orElse(null);
    }

    private BpmNode firstPending(List<BpmNode> nodes) {
        return nodes.stream()
                .filter(n -> n.getStatus() == BpmNodeStatus.PENDING)
                .min(Comparator.comparing(BpmNode::getSeqNo))
                .orElse(null);
    }

    private BpmNode nextPendingAfter(List<BpmNode> nodes, int fromSeq) {
        return nodes.stream()
                .filter(n -> n.getStatus() == BpmNodeStatus.PENDING)
                .filter(n -> n.getSeqNo() > fromSeq)
                .min(Comparator.comparing(BpmNode::getSeqNo))
                .orElse(null);
    }

    /** 到达审批节点：解析审批人并生成任务；解析为空时落 PENDING_ASSIGNEE 并告警。 */
    private void activate(
            BpmInstance instance,
            BpmNode node,
            BpmSchemaNode def,
            List<BpmResolvedAssignee> resolved) {
        if (resolved.isEmpty()) {
            log.warn(
                    "审批人解析为空：instanceId={}，node={}，任务置 PENDING_ASSIGNEE，不生成无人待办",
                    instance.getId(),
                    node.getNodeKey());
            node.setStatus(BpmNodeStatus.ACTIVE);
            node.setArrivedAt(LocalDateTime.now());
            persistNodeOrThrow(node);
            createTask(instance, node, null, primarySource(def), BpmTaskStatus.PENDING_ASSIGNEE);
            return;
        }
        node.setStatus(BpmNodeStatus.ACTIVE);
        node.setArrivedAt(LocalDateTime.now());
        persistNodeOrThrow(node);
        int dueHours =
                def.getDueHours() == null
                        ? Math.max(bpmProperties.getDefaultDueHours(), 1)
                        : Math.max(def.getDueHours(), 1);
        activateWithAssignees(instance, node, resolved, dueHours);
    }

    private void activateWithAssignees(
            BpmInstance instance,
            BpmNode node,
            List<BpmResolvedAssignee> resolved,
            int dueHours) {
        if (resolved == null || resolved.isEmpty()) {
            log.warn(
                    "审批人解析为空：instanceId={}，node={}，任务置 PENDING_ASSIGNEE",
                    instance.getId(),
                    node.getNodeKey());
            createTask(
                    instance,
                    node,
                    null,
                    BpmAssigneeSource.USER,
                    BpmTaskStatus.PENDING_ASSIGNEE,
                    dueHours);
            return;
        }
        for (BpmResolvedAssignee assignee : resolved) {
            insertSnapshot(node, assignee);
            createTask(
                    instance,
                    node,
                    assignee.getAssigneeId(),
                    assignee.getSourceType(),
                    BpmTaskStatus.PENDING,
                    dueHours);
        }
    }

    private void insertSnapshot(BpmNode node, BpmResolvedAssignee assignee) {
        BpmAssigneeSnapshot snapshot = new BpmAssigneeSnapshot();
        snapshot.setNodeId(node.getId());
        snapshot.setAssigneeId(assignee.getAssigneeId());
        snapshot.setSourceType(assignee.getSourceType());
        snapshot.setResolvedAt(LocalDateTime.now());
        snapshotMapper.insert(snapshot);
    }

    private void createTask(
            BpmInstance instance,
            BpmNode node,
            Long assigneeId,
            BpmAssigneeSource sourceType,
            BpmTaskStatus status) {
        createTask(instance, node, assigneeId, sourceType, status, bpmProperties.getDefaultDueHours());
    }

    private void createTask(
            BpmInstance instance,
            BpmNode node,
            Long assigneeId,
            BpmAssigneeSource sourceType,
            BpmTaskStatus status,
            int dueHours) {
        BpmTask task = new BpmTask();
        task.setInstanceId(instance.getId());
        task.setNodeId(node.getId());
        task.setAssigneeId(assigneeId);
        task.setSourceType(sourceType == null ? BpmAssigneeSource.USER : sourceType);
        task.setTaskType(TaskType.APPROVE);
        task.setStatus(status);
        task.setDueAt(LocalDateTime.now().plusHours(Math.max(dueHours, 1)));
        applyProxy(instance, task);
        taskMapper.insert(task);
        // 待办生成通知：通知任务处理人（驳回给发起人同样走任务创建，自动覆盖）
        notificationPublisher.onTaskCreated(instance, task);
    }

    /**
     * 待办代理：审批人处于代理时段时，任务转交代理人处理。
     *
     * <p>assignee_id 写代理人（待办/权限判断均以该字段为准），owner_assignee_id 保留原审批人，
     * 来源标记为 PROXY 以便前端展示“代理人（代 XXX）”。仅对待处理任务生效。
     */
    private void applyProxy(BpmInstance instance, BpmTask task) {
        if (task.getStatus() != BpmTaskStatus.PENDING || task.getAssigneeId() == null) {
            return;
        }
        Long proxyId =
                proxyResolver.resolve(task.getAssigneeId(), instance.getBizType(), LocalDateTime.now());
        if (proxyId == null || proxyId.equals(task.getAssigneeId())) {
            return;
        }
        log.info(
                "BPM 待办代理：instanceId={}，nodeId={}，原审批人={}，代理人={}",
                instance.getId(),
                task.getNodeId(),
                task.getAssigneeId(),
                proxyId);
        task.setOwnerAssigneeId(task.getAssigneeId());
        task.setAssigneeId(proxyId);
        task.setSourceType(BpmAssigneeSource.PROXY);
    }

    /** 判断审批节点是否满足自动通过规则（命中则不生成待办，直接完成节点）。 */
    private boolean shouldAutoApprove(
            BpmInstance instance, BpmSchemaNode def, List<BpmResolvedAssignee> resolved) {
        if (def == null || resolved == null || resolved.isEmpty()) {
            return false;
        }
        if (Boolean.TRUE.equals(def.getAutoApproveIfInitiator())) {
            Long initiatorId = instance.getInitiatorId();
            boolean onlyInitiator =
                    initiatorId != null
                            && resolved.stream()
                                    .allMatch(a -> initiatorId.equals(a.getAssigneeId()));
            if (onlyInitiator) {
                return true;
            }
        }
        List<BpmSchemaCondition> autoConditions = def.getAutoApproveConditions();
        if (autoConditions == null || autoConditions.isEmpty()) {
            return false;
        }
        BpmConditionContext context = conditionContextBuilder.build(instance);
        return autoConditions.stream().anyMatch(c -> conditionEvaluator.matches(c, context));
    }

    /** 自动通过：直接完成节点并留痕（任务记为 APPROVED，不发送待办通知）。 */
    private void autoApprove(
            BpmInstance instance, BpmNode node, List<BpmResolvedAssignee> resolved) {
        LocalDateTime now = LocalDateTime.now();
        node.setStatus(BpmNodeStatus.COMPLETED);
        node.setArrivedAt(now);
        node.setCompletedAt(now);
        persistNodeOrThrow(node);
        for (BpmResolvedAssignee assignee : resolved) {
            insertSnapshot(node, assignee);
            insertAutoTask(instance, node, assignee);
        }
        bizEventDispatcher.onNodeCompleted(instance, node.getNodeKey());
        log.info(
                "BPM 节点自动通过：instanceId={}，node={}，审批人数={}",
                instance.getId(),
                node.getNodeKey(),
                resolved.size());
    }

    private void insertAutoTask(
            BpmInstance instance, BpmNode node, BpmResolvedAssignee assignee) {
        BpmTask task = new BpmTask();
        task.setInstanceId(instance.getId());
        task.setNodeId(node.getId());
        task.setAssigneeId(assignee.getAssigneeId());
        task.setSourceType(assignee.getSourceType());
        task.setTaskType(TaskType.APPROVE);
        task.setStatus(BpmTaskStatus.APPROVED);
        task.setDueAt(LocalDateTime.now());
        taskMapper.insert(task);
    }

    private void writeCcRecords(BpmInstance instance, BpmNode node, BpmSchemaNode def) {
        List<BpmResolvedAssignee> resolved =
                assigneeResolver.resolve(
                        def,
                        instance.getInitiatorId(),
                        BpmBizType.from(instance.getBizType()),
                        instance.getBizId());
        if (resolved.isEmpty()) {
            log.warn("抄送人解析为空：instanceId={}，node={}", instance.getId(), node.getNodeKey());
            return;
        }
        for (BpmResolvedAssignee assignee : resolved) {
            BpmCcRecord record = new BpmCcRecord();
            record.setInstanceId(instance.getId());
            record.setNodeId(node.getId());
            record.setReceiverId(assignee.getAssigneeId());
            record.setReadStatus(CcReadStatus.UNREAD);
            ccRecordMapper.insert(record);
            // 抄送通知：通知抄送接收人
            notificationPublisher.onCcCreated(instance, record);
        }
    }

    private void autoComplete(BpmNode node) {
        node.setArrivedAt(LocalDateTime.now());
        node.setStatus(BpmNodeStatus.COMPLETED);
        node.setCompletedAt(LocalDateTime.now());
        persistNodeOrThrow(node);
    }

    private BpmNode resolveRejectNode(
            BpmInstance instance, List<BpmNode> nodes, BpmNode current, RejectTarget target) {
        if (target == null || target == RejectTarget.INITIATOR) {
            // 驳回发起人：回到 START 节点，重新生成发起人待办
            return nodes.stream()
                    .filter(n -> n.getNodeType() == BpmNodeType.START)
                    .min(Comparator.comparing(BpmNode::getSeqNo))
                    .orElseGet(
                            () -> nodes.stream().min(Comparator.comparing(BpmNode::getSeqNo)).orElse(null));
        }
        // 驳回上一级：只回溯到上一个审批节点，跳过 CC/CONDITION/AUTO/END 等非审批节点
        return nodes.stream()
                .filter(n -> n.getSeqNo() < current.getSeqNo())
                .filter(n -> n.getNodeType() == BpmNodeType.SINGLE
                        || n.getNodeType() == BpmNodeType.COUNTERSIGN
                        || n.getNodeType() == BpmNodeType.OR_SIGN)
                .max(Comparator.comparing(BpmNode::getSeqNo))
                .orElse(null);
    }

    private BpmResolvedAssignee initiatorAssignee(BpmInstance instance) {
        BpmResolvedAssignee assignee = new BpmResolvedAssignee();
        assignee.setAssigneeId(instance.getInitiatorId());
        assignee.setSourceType(BpmAssigneeSource.INITIATOR);
        return assignee;
    }

    /** 实例收口：关闭在途任务与未完成节点，并更新实例终态。 */
    private void finish(BpmInstance instance, List<BpmNode> nodes, BpmInstanceStatus status) {
        closePendingTasks(instance);
        for (BpmNode node : nodes) {
            if (node.getStatus() == BpmNodeStatus.ACTIVE || node.getStatus() == BpmNodeStatus.PENDING) {
                node.setStatus(BpmNodeStatus.CLOSED);
                nodeMapper.updateById(node);
            }
        }
        finishInstanceOnly(instance, status);
    }

    private void closePendingTasks(BpmInstance instance) {
        List<BpmTask> tasks =
                taskMapper.selectList(
                        new LambdaQueryWrapper<BpmTask>()
                                .eq(BpmTask::getInstanceId, instance.getId())
                                .in(
                                        BpmTask::getStatus,
                                        BpmTaskStatus.PENDING,
                                        BpmTaskStatus.PENDING_ASSIGNEE));
        closeTasks(tasks);
    }

    private void closeTasks(List<BpmTask> tasks) {
        for (BpmTask task : tasks) {
            task.setStatus(BpmTaskStatus.CLOSED);
            taskMapper.updateById(task);
        }
    }

    private void finishInstanceOnly(BpmInstance instance, BpmInstanceStatus status) {
        instance.setStatus(status);
        instance.setEndedAt(LocalDateTime.now());
        if (instanceMapper.updateById(instance) == 0) {
            BpmInstance latest = instanceMapper.selectById(instance.getId());
            if (latest != null && latest.getStatus() == status) {
                // 并发场景下其他请求已完成收口：不重复通知发起人
                return;
            }
            throw BusinessException.of(ErrorCode.BPM_INSTANCE_CONFLICT, "实例已被其他操作更新，请刷新重试");
        }
        // 实例结束通知：仅在实际完成状态更新后发送一次（finish 与 finishInstanceOnly 共用此出口）
        notificationPublisher.onInstanceFinished(instance, status);
        // 实例终态事件：业务侧据此把终态写回业务单据（审批状态唯一来源是实例）
        bizEventDispatcher.onInstanceFinished(instance, status);
    }

    private void touchInstance(BpmInstance instance) {
        if (instanceMapper.updateById(instance) == 0) {
            throw BusinessException.of(ErrorCode.BPM_INSTANCE_CONFLICT, "实例已被其他操作更新，请刷新重试");
        }
    }

    private boolean persistNode(BpmNode node) {
        return nodeMapper.updateById(node) > 0;
    }

    private void persistNodeOrThrow(BpmNode node) {
        if (!persistNode(node)) {
            throw BusinessException.of(ErrorCode.BPM_INSTANCE_CONFLICT, "节点状态已变更，请刷新重试");
        }
    }

    private BpmAssigneeSource primarySource(BpmSchemaNode def) {
        if (def != null && def.getAssignees() != null && !def.getAssignees().isEmpty()) {
            return def.getAssignees().get(0).getType();
        }
        return BpmAssigneeSource.USER;
    }

    private int resolveDueHours(BpmSchema schema, BpmNode node) {
        if (schema != null && node != null) {
            BpmSchemaNode def = schema.findByKey(node.getNodeKey());
            if (def != null && def.getDueHours() != null) {
                return Math.max(def.getDueHours(), 1);
            }
        }
        return Math.max(bpmProperties.getDefaultDueHours(), 1);
    }

    /** 供实例服务判断首节点是否尚未处理：存在已处理任务即视为已处理。 */
    public boolean hasProcessedTask(Long instanceId) {
        return taskMapper.selectCount(
                        new LambdaQueryWrapper<BpmTask>()
                                .eq(BpmTask::getInstanceId, instanceId)
                                .notIn(
                                        BpmTask::getStatus,
                                        BpmTaskStatus.PENDING,
                                        BpmTaskStatus.PENDING_ASSIGNEE))
                > 0;
    }

    /** 读取实例全部节点（按顺序号升序）。 */
    public List<BpmNode> loadNodes(Long instanceId) {
        return new ArrayList<>(
                nodeMapper.selectList(
                        new LambdaQueryWrapper<BpmNode>()
                                .eq(BpmNode::getInstanceId, instanceId)
                                .orderByAsc(BpmNode::getSeqNo)));
    }
}
