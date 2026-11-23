package com.ceams.module.bpm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.common.response.PageResult;
import com.ceams.module.bpm.dto.TaskApproveRequest;
import com.ceams.module.bpm.dto.TaskDelegateRequest;
import com.ceams.module.bpm.dto.TaskRejectRequest;
import com.ceams.module.bpm.entity.BpmCcRecord;
import com.ceams.module.bpm.entity.BpmInstance;
import com.ceams.module.bpm.entity.BpmNode;
import com.ceams.module.bpm.entity.BpmTask;
import com.ceams.module.bpm.entity.BpmTemplateVersion;
import com.ceams.module.bpm.enums.AuditAction;
import com.ceams.module.bpm.enums.BpmDataScope;
import com.ceams.module.bpm.enums.BpmInstanceStatus;
import com.ceams.module.bpm.enums.BpmNodeType;
import com.ceams.module.bpm.enums.BpmTaskStatus;
import com.ceams.module.bpm.enums.CcReadStatus;
import com.ceams.module.bpm.enums.TaskType;
import com.ceams.module.bpm.mapper.BpmCcRecordMapper;
import com.ceams.module.bpm.mapper.BpmInstanceMapper;
import com.ceams.module.bpm.mapper.BpmNodeMapper;
import com.ceams.module.bpm.mapper.BpmTaskMapper;
import com.ceams.module.bpm.mapper.BpmTemplateVersionMapper;
import com.ceams.module.bpm.service.BpmAuditService;
import com.ceams.module.bpm.service.BpmInstanceService;
import com.ceams.module.bpm.service.BpmTaskService;
import com.ceams.module.bpm.support.BpmFlowEngine;
import com.ceams.module.bpm.support.BpmNotificationPublisher;
import com.ceams.module.bpm.support.BpmSchema;
import com.ceams.module.bpm.support.BpmSchemaNode;
import com.ceams.module.bpm.support.BpmSchemaParser;
import com.ceams.module.bpm.support.BpmSecurityGuard;
import com.ceams.module.bpm.support.BpmViewAssembler;
import com.ceams.module.bpm.vo.CcRecordVO;
import com.ceams.module.bpm.vo.InstanceVO;
import com.ceams.module.bpm.vo.TaskVO;
import com.ceams.module.iam.entity.SysUser;
import com.ceams.module.iam.enums.UserStatus;
import com.ceams.module.iam.mapper.UserMapper;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** 审批任务服务实现。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BpmTaskServiceImpl implements BpmTaskService {

    private final BpmTaskMapper taskMapper;
    private final BpmNodeMapper nodeMapper;
    private final BpmInstanceMapper instanceMapper;
    private final BpmTemplateVersionMapper versionMapper;
    private final BpmCcRecordMapper ccRecordMapper;
    private final UserMapper userMapper;
    private final BpmSchemaParser schemaParser;
    private final BpmFlowEngine flowEngine;
    private final BpmAuditService auditService;
    private final BpmSecurityGuard securityGuard;
    private final BpmViewAssembler assembler;
    private final BpmInstanceService instanceService;
    private final BpmNotificationPublisher notificationPublisher;

    // ------------------------------------------------------------------
    // 四个查询
    // ------------------------------------------------------------------

    @Override
    public PageResult<TaskVO> pagePending(
            long page, long size, String bizType, Boolean overdue) {
        Long current = requireLogin();
        LambdaQueryWrapper<BpmTask> wrapper = new LambdaQueryWrapper<>();
        // 数据范围：SELF=仅本人；DEPT=同部门及子部门用户；ALL=管理员不限制
        applyAssigneeScope(wrapper, current);
        wrapper.eq(BpmTask::getStatus, BpmTaskStatus.PENDING);
        if (Boolean.TRUE.equals(overdue)) {
            wrapper.lt(BpmTask::getDueAt, LocalDateTime.now());
        }
        List<Long> instanceIds = resolveInstanceIds(bizType);
        if (instanceIds != null) {
            if (instanceIds.isEmpty()) {
                return emptyPage();
            }
            wrapper.in(BpmTask::getInstanceId, instanceIds);
        }
        wrapper.orderByAsc(BpmTask::getDueAt).orderByDesc(BpmTask::getCreateTime);
        return pageTasks(page, size, wrapper);
    }

    @Override
    public PageResult<InstanceVO> pageInitiated(
            long page, long size, BpmInstanceStatus status, String bizType) {
        return instanceService.pageByInitiator(page, size, status, bizType);
    }

    @Override
    public PageResult<TaskVO> pageDone(long page, long size, String bizType) {
        Long current = requireLogin();
        LambdaQueryWrapper<BpmTask> wrapper = new LambdaQueryWrapper<>();
        // 数据范围：SELF=仅本人；DEPT=同部门及子部门用户；ALL=管理员不限制
        applyAssigneeScope(wrapper, current);
        wrapper.in(BpmTask::getStatus, BpmTaskStatus.APPROVED, BpmTaskStatus.REJECTED);
        List<Long> instanceIds = resolveInstanceIds(bizType);
        if (instanceIds != null) {
            if (instanceIds.isEmpty()) {
                return emptyPage();
            }
            wrapper.in(BpmTask::getInstanceId, instanceIds);
        }
        wrapper.orderByDesc(BpmTask::getUpdateTime);
        return pageTasks(page, size, wrapper);
    }

    @Override
    public PageResult<CcRecordVO> pageCc(
            long page, long size, String bizType, CcReadStatus readStatus) {
        Long current = requireLogin();
        List<Long> instanceIds = resolveInstanceIds(bizType);
        if (instanceIds != null && instanceIds.isEmpty()) {
            return emptyPage();
        }
        Page<BpmCcRecord> p = new Page<>(Math.max(page, 1), Math.min(Math.max(size, 1), 100));
        LambdaQueryWrapper<BpmCcRecord> wrapper = new LambdaQueryWrapper<>();
        // 数据范围：SELF=仅本人；DEPT=同部门及子部门用户；ALL=管理员不限制
        applyReceiverScope(wrapper, current);
        wrapper.eq(readStatus != null, BpmCcRecord::getReadStatus, readStatus);
        wrapper.in(instanceIds != null, BpmCcRecord::getInstanceId, instanceIds);
        wrapper.orderByDesc(BpmCcRecord::getCreateTime);
        IPage<BpmCcRecord> result = ccRecordMapper.selectPage(p, wrapper);
        List<BpmCcRecord> records = result.getRecords();
        if (records.isEmpty()) {
            return new PageResult<>(List.of(), result.getTotal(), result.getCurrent(), result.getSize());
        }
        Map<Long, BpmInstance> instanceMap =
                assembler.index(
                        instanceMapper.selectBatchIds(
                                records.stream().map(BpmCcRecord::getInstanceId).distinct().toList()),
                        BpmInstance::getId);
        Map<Long, BpmNode> nodeMap =
                assembler.index(
                        nodeMapper.selectBatchIds(
                                records.stream().map(BpmCcRecord::getNodeId).distinct().toList()),
                        BpmNode::getId);
        // 批量装配展示字段：发起人一次查询、模板版本一次查询后解析 schema，避免 N+1
        Map<Long, SysUser> userMap =
                loadUsersByIds(collectInitiatorIds(instanceMap.values()));
        Map<Long, Map<String, String>> nodeNames = loadNodeNames(instanceMap.values());
        List<CcRecordVO> vos =
                records.stream()
                        .map(
                                r ->
                                        enrichCcRecordVO(
                                                r, instanceMap, nodeMap, userMap, nodeNames))
                        .toList();
        return new PageResult<>(vos, result.getTotal(), result.getCurrent(), result.getSize());
    }

    // ------------------------------------------------------------------
    // 同意 / 驳回 / 改派
    // ------------------------------------------------------------------

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long taskId, TaskApproveRequest request) {
        BpmTask task = requireTask(taskId);
        securityGuard.requireTaskOwner(task.getAssigneeId());
        if (task.getStatus() == BpmTaskStatus.APPROVED) {
            // 同一任务重复提交同一动作：返回首次结果（幂等）
            return;
        }
        if (task.getStatus() != BpmTaskStatus.PENDING) {
            throw BusinessException.of(ErrorCode.BPM_TASK_CONFLICT, "任务已处理，不能重复同意");
        }
        BpmInstance instance = requireInstance(task.getInstanceId());
        requireVersions(task, instance, request.getTaskVersion(), request.getInstanceVersion());
        BpmNode node = requireNode(task.getNodeId());
        // 锁定节点行，串行化同节点并发审批（会签/或签最后一人才能推进节点）
        flowEngine.lockNode(node.getId());
        task = requireTask(taskId);
        if (task.getStatus() != BpmTaskStatus.PENDING) {
            throw BusinessException.of(ErrorCode.BPM_TASK_CONFLICT, "任务已被他人处理，请刷新后重试");
        }
        requireVersions(task, instance, request.getTaskVersion(), request.getInstanceVersion());
        Map<String, Object> before = snapshotTask(task);

        task.setStatus(BpmTaskStatus.APPROVED);
        updateTaskOrConflict(task);

        if (node.getNodeType() == BpmNodeType.OR_SIGN) {
            // 或签：任一同意即流转，关闭同节点其他待办
            closeOtherPendingTasks(node.getId(), task.getId());
        } else if (node.getNodeType() == BpmNodeType.COUNTERSIGN) {
            long pendingOthers = countPendingOthers(node.getId(), task.getId());
            if (pendingOthers > 0) {
                // 会签：还有人未处理，节点保持 ACTIVE
                auditService.record(
                        BpmAuditService.TARGET_TASK,
                        task.getId(),
                        AuditAction.APPROVE,
                        before,
                        task,
                        null,
                        securityGuard.currentUserId());
                return;
            }
        }

        BpmSchema schema = loadSchema(instance);
        List<BpmNode> nodes = flowEngine.loadNodes(instance.getId());
        flowEngine.afterApprove(instance, nodes, node, schema);
        auditService.record(
                BpmAuditService.TARGET_TASK,
                task.getId(),
                AuditAction.APPROVE,
                before,
                task,
                null,
                securityGuard.currentUserId());
        log.info(
                "BPM 同意：taskId={}，instanceId={}，node={}，operator={}",
                taskId,
                instance.getId(),
                node.getNodeKey(),
                securityGuard.currentUserId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(Long taskId, TaskRejectRequest request) {
        BpmTask task = requireTask(taskId);
        securityGuard.requireTaskOwner(task.getAssigneeId());
        if (task.getStatus() == BpmTaskStatus.REJECTED) {
            // 同一任务重复提交同一动作：返回首次结果（幂等）
            return;
        }
        if (task.getStatus() != BpmTaskStatus.PENDING) {
            throw BusinessException.of(ErrorCode.BPM_TASK_CONFLICT, "任务已处理，不能重复驳回");
        }
        BpmInstance instance = requireInstance(task.getInstanceId());
        requireVersions(task, instance, request.getTaskVersion(), request.getInstanceVersion());
        BpmNode node = requireNode(task.getNodeId());
        // 锁定节点行，串行化同节点并发审批，避免会签最后一人与他人驳回并发导致终态不一致
        flowEngine.lockNode(node.getId());
        task = requireTask(taskId);
        if (task.getStatus() != BpmTaskStatus.PENDING) {
            throw BusinessException.of(ErrorCode.BPM_TASK_CONFLICT, "任务已被他人处理，请刷新后重试");
        }
        requireVersions(task, instance, request.getTaskVersion(), request.getInstanceVersion());
        Map<String, Object> before = snapshotTask(task);

        task.setStatus(BpmTaskStatus.REJECTED);
        updateTaskOrConflict(task);

        // 任一驳回即整体驳回：关闭同节点其他待办后按目标节点回退。
        // 或签与“同意”保持对称（任一人处理即出结果），避免出现“一人驳回、他人同意”
        // 时流程继续向后流转，导致已驳回的节点被忽略、查看流程时看不到驳回与回退。
        closeOtherPendingTasks(node.getId(), task.getId());

        BpmSchema schema = loadSchema(instance);
        List<BpmNode> nodes = flowEngine.loadNodes(instance.getId());
        flowEngine.afterReject(instance, nodes, node, schema, request.getTarget());
        auditService.record(
                BpmAuditService.TARGET_TASK,
                task.getId(),
                AuditAction.REJECT,
                before,
                task,
                null,
                securityGuard.currentUserId());
        log.info(
                "BPM 驳回：taskId={}，instanceId={}，node={}，target={}，operator={}",
                taskId,
                instance.getId(),
                node.getNodeKey(),
                request.getTarget(),
                securityGuard.currentUserId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delegate(Long taskId, TaskDelegateRequest request) {
        // 管理员或具备 bpm:task:delegate 权限的用户可改派
        securityGuard.requireAdminOrPermission("bpm:task:delegate");
        BpmTask task = requireTask(taskId);
        if (task.getStatus() != BpmTaskStatus.PENDING
                && task.getStatus() != BpmTaskStatus.PENDING_ASSIGNEE) {
            throw BusinessException.of(ErrorCode.BPM_TASK_CONFLICT, "仅待处理任务可改派");
        }
        BpmInstance instance = requireInstance(task.getInstanceId());
        requireVersions(task, instance, request.getTaskVersion(), request.getInstanceVersion());
        BpmNode node = requireNode(task.getNodeId());
        SysUser targetUser = userMapper.selectById(request.getTargetUserId());
        if (targetUser == null || targetUser.getStatus() != UserStatus.ACTIVE) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "目标处理人不存在或已停用");
        }
        Map<String, Object> before = snapshotTask(task);

        task.setStatus(BpmTaskStatus.CLOSED);
        updateTaskOrConflict(task);

        BpmTask newTask = new BpmTask();
        newTask.setInstanceId(task.getInstanceId());
        newTask.setNodeId(task.getNodeId());
        newTask.setAssigneeId(targetUser.getId());
        newTask.setSourceType(com.ceams.module.bpm.enums.BpmAssigneeSource.DELEGATE);
        newTask.setTaskType(TaskType.APPROVE);
        newTask.setStatus(BpmTaskStatus.PENDING);
        newTask.setDueAt(
                task.getDueAt() == null
                        ? LocalDateTime.now().plusHours(24)
                        : task.getDueAt());
        taskMapper.insert(newTask);
        flowEngine.recordDelegateSnapshot(node, targetUser.getId());
        // 改派通知：通知新处理人
        notificationPublisher.onTaskDelegated(instance, newTask);

        auditService.record(
                BpmAuditService.TARGET_TASK,
                taskId,
                AuditAction.DELEGATE,
                before,
                newTask,
                null,
                securityGuard.currentUserId());
        log.info(
                "BPM 改派：taskId={}，instanceId={}，from={}，to={}，operator={}",
                taskId,
                task.getInstanceId(),
                task.getAssigneeId(),
                targetUser.getId(),
                securityGuard.currentUserId());
    }

    // ------------------------------------------------------------------
    // 内部工具
    // ------------------------------------------------------------------

    private PageResult<TaskVO> pageTasks(
            long page, long size, LambdaQueryWrapper<BpmTask> wrapper) {
        Page<BpmTask> p = new Page<>(Math.max(page, 1), Math.min(Math.max(size, 1), 100));
        IPage<BpmTask> result = taskMapper.selectPage(p, wrapper);
        List<BpmTask> records = result.getRecords();
        if (records.isEmpty()) {
            return new PageResult<>(List.of(), result.getTotal(), result.getCurrent(), result.getSize());
        }
        Map<Long, BpmInstance> instanceMap =
                assembler.index(
                        instanceMapper.selectBatchIds(
                                records.stream().map(BpmTask::getInstanceId).distinct().toList()),
                        BpmInstance::getId);
        Map<Long, BpmNode> nodeMap =
                assembler.index(
                        nodeMapper.selectBatchIds(
                                records.stream().map(BpmTask::getNodeId).distinct().toList()),
                        BpmNode::getId);
        // 批量装配展示字段：处理人 + 发起人一次批量查询；模板版本一次批量查询后解析 schema
        Set<Long> userIds = collectInitiatorIds(instanceMap.values());
        records.stream()
                .map(BpmTask::getAssigneeId)
                .filter(Objects::nonNull)
                .forEach(userIds::add);
        records.stream()
                .map(BpmTask::getOwnerAssigneeId)
                .filter(Objects::nonNull)
                .forEach(userIds::add);
        Map<Long, SysUser> userMap = loadUsersByIds(userIds);
        Map<Long, Map<String, String>> nodeNames = loadNodeNames(instanceMap.values());
        List<TaskVO> vos =
                records.stream()
                        .map(t -> enrichTaskVO(t, instanceMap, nodeMap, userMap, nodeNames))
                        .toList();
        return new PageResult<>(vos, result.getTotal(), result.getCurrent(), result.getSize());
    }

    /** 待办/已办视图装配：补发起人姓名与当前节点名称（不改变审批动作与权限逻辑）。 */
    private TaskVO enrichTaskVO(
            BpmTask task,
            Map<Long, BpmInstance> instanceMap,
            Map<Long, BpmNode> nodeMap,
            Map<Long, SysUser> userMap,
            Map<Long, Map<String, String>> nodeNames) {
        TaskVO vo = assembler.toTaskVO(task, instanceMap, nodeMap);
        if (task.getOwnerAssigneeId() != null) {
            vo.setOwnerAssigneeName(displayName(userMap.get(task.getOwnerAssigneeId())));
        }
        BpmInstance instance = instanceMap.get(task.getInstanceId());
        if (instance != null) {
            vo.setInitiatorName(displayName(userMap.get(instance.getInitiatorId())));
            vo.setCurrentNodeName(resolveNodeName(instance, nodeMap.get(task.getNodeId()), nodeNames));
        }
        return vo;
    }

    /** 抄送视图装配：补发起人与当前节点名称。 */
    private CcRecordVO enrichCcRecordVO(
            BpmCcRecord record,
            Map<Long, BpmInstance> instanceMap,
            Map<Long, BpmNode> nodeMap,
            Map<Long, SysUser> userMap,
            Map<Long, Map<String, String>> nodeNames) {
        CcRecordVO vo = assembler.toCcRecordVO(record, instanceMap, nodeMap);
        BpmInstance instance = instanceMap.get(record.getInstanceId());
        if (instance != null) {
            vo.setInitiatorId(instance.getInitiatorId());
            vo.setInitiatorName(displayName(userMap.get(instance.getInitiatorId())));
            vo.setCurrentNodeName(resolveNodeName(instance, nodeMap.get(record.getNodeId()), nodeNames));
        }
        return vo;
    }

    /** 收集实例发起人 ID（用于批量查询用户姓名）。 */
    private Set<Long> collectInitiatorIds(Collection<BpmInstance> instances) {
        Set<Long> ids = new HashSet<>();
        for (BpmInstance instance : instances) {
            if (instance.getInitiatorId() != null) {
                ids.add(instance.getInitiatorId());
            }
        }
        return ids;
    }

    /** 批量查询用户（一次 IN 查询，避免逐行查询造成 N+1）。 */
    private Map<Long, SysUser> loadUsersByIds(Collection<Long> userIds) {
        Set<Long> ids =
                userIds.stream().filter(Objects::nonNull).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Map.of();
        }
        List<SysUser> users = userMapper.selectBatchIds(ids);
        if (users == null || users.isEmpty()) {
            return Map.of();
        }
        return users.stream()
                .filter(user -> user.getId() != null)
                .collect(Collectors.toMap(SysUser::getId, user -> user, (a, b) -> a));
    }

    /**
     * 批量解析模板版本 schema 的「节点 key -> 节点名称」映射。
     *
     * <p>同一版本只解析一次；解析失败仅告警并降级为空映射，不影响列表查询。
     */
    private Map<Long, Map<String, String>> loadNodeNames(Collection<BpmInstance> instances) {
        Set<Long> versionIds =
                instances.stream()
                        .map(BpmInstance::getTemplateVersionId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
        if (versionIds.isEmpty()) {
            return Map.of();
        }
        List<BpmTemplateVersion> versions = versionMapper.selectBatchIds(versionIds);
        if (versions == null || versions.isEmpty()) {
            return Map.of();
        }
        Map<Long, Map<String, String>> result = new HashMap<>();
        for (BpmTemplateVersion version : versions) {
            Map<String, String> names = new HashMap<>();
            try {
                BpmSchema schema = schemaParser.parse(version.getSchemaJson());
                for (BpmSchemaNode def : schema.getNodes()) {
                    if (def.getNodeKey() != null && def.getNodeName() != null) {
                        names.put(def.getNodeKey(), def.getNodeName());
                    }
                }
            } catch (Exception e) {
                log.warn(
                        "待办视图解析流程模板失败，节点名称降级为空：versionId={}，原因={}",
                        version.getId(),
                        e.getMessage());
            }
            result.put(version.getId(), names);
        }
        return result;
    }

    /** 按实例版本与节点 key 取节点名称，缺省返回 null（前端可回退展示节点 key）。 */
    private String resolveNodeName(
            BpmInstance instance,
            BpmNode node,
            Map<Long, Map<String, String>> nodeNames) {
        if (instance == null || node == null) {
            return null;
        }
        Map<String, String> names = nodeNames.get(instance.getTemplateVersionId());
        return names == null ? null : names.get(node.getNodeKey());
    }

    private String displayName(SysUser user) {
        return user == null ? null : user.getName();
    }

    private <T> PageResult<T> emptyPage() {
        return new PageResult<>(List.of(), 0, 1, 0);
    }

    private List<Long> resolveInstanceIds(String bizType) {
        if (!StringUtils.hasText(bizType)) {
            return null;
        }
        return instanceMapper
                .selectList(
                        new LambdaQueryWrapper<BpmInstance>()
                                .select(BpmInstance::getId)
                                .eq(BpmInstance::getBizType, bizType))
                .stream()
                .map(BpmInstance::getId)
                .toList();
    }

    private BpmTask requireTask(Long taskId) {
        BpmTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "审批任务不存在");
        }
        return task;
    }

    private BpmInstance requireInstance(Long instanceId) {
        BpmInstance instance = instanceMapper.selectById(instanceId);
        if (instance == null) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "流程实例不存在");
        }
        return instance;
    }

    private BpmNode requireNode(Long nodeId) {
        BpmNode node = nodeMapper.selectById(nodeId);
        if (node == null) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "流程节点不存在");
        }
        return node;
    }

    private BpmSchema loadSchema(BpmInstance instance) {
        BpmTemplateVersion version = versionMapper.selectById(instance.getTemplateVersionId());
        if (version == null) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "流程模板版本不存在");
        }
        return schemaParser.parse(version.getSchemaJson());
    }

    /** 校验 taskVersion/instanceVersion，明确区分任务冲突与实例冲突错误码。 */
    private void requireVersions(
            BpmTask task, BpmInstance instance, Integer taskVersion, Integer instanceVersion) {
        if (taskVersion == null || !taskVersion.equals(task.getVersion())) {
            throw BusinessException.of(ErrorCode.BPM_TASK_CONFLICT, "任务版本已变更，请刷新后重试");
        }
        // taskVersion 为必传；instanceVersion 为可选加固，传了才校验，避免客户端多字段负担
        if (instanceVersion != null && !instanceVersion.equals(instance.getVersion())) {
            throw BusinessException.of(ErrorCode.BPM_INSTANCE_CONFLICT, "实例版本已变更，请刷新后重试");
        }
    }

    /** 乐观锁更新任务；更新失败返回明确任务冲突错误码。 */
    private void updateTaskOrConflict(BpmTask task) {
        if (taskMapper.updateById(task) == 0) {
            throw BusinessException.of(ErrorCode.BPM_TASK_CONFLICT, "任务已被他人处理，请刷新后重试");
        }
    }

    private long countPendingOthers(Long nodeId, Long excludeTaskId) {
        return taskMapper.selectCount(
                new LambdaQueryWrapper<BpmTask>()
                        .eq(BpmTask::getNodeId, nodeId)
                        .eq(BpmTask::getStatus, BpmTaskStatus.PENDING)
                        .ne(BpmTask::getId, excludeTaskId));
    }

    private void closeOtherPendingTasks(Long nodeId, Long excludeTaskId) {
        BpmTask patch = new BpmTask();
        patch.setStatus(BpmTaskStatus.CLOSED);
        taskMapper.update(
                patch,
                new LambdaUpdateWrapper<BpmTask>()
                        .eq(BpmTask::getNodeId, nodeId)
                        .eq(BpmTask::getStatus, BpmTaskStatus.PENDING)
                        .ne(BpmTask::getId, excludeTaskId));
    }

    private Map<String, Object> snapshotTask(BpmTask task) {
        Map<String, Object> before = new LinkedHashMap<>();
        before.put("id", task.getId());
        before.put("instanceId", task.getInstanceId());
        before.put("nodeId", task.getNodeId());
        before.put("assigneeId", task.getAssigneeId());
        before.put("status", task.getStatus());
        before.put("version", task.getVersion());
        return before;
    }

    private Long requireLogin() {
        Long current = securityGuard.currentUserId();
        if (current == null) {
            throw BusinessException.of(ErrorCode.AUTH_ACCESS_DENIED, "未登录不可查询");
        }
        return current;
    }

    /**
     * 待办/已办数据范围：ALL 不过滤；DEPT 同部门及子部门用户；SELF（含范围未知）仅本人。
     */
    private void applyAssigneeScope(LambdaQueryWrapper<BpmTask> wrapper, Long current) {
        BpmDataScope scope = securityGuard.resolveDataScope();
        if (scope == BpmDataScope.ALL) {
            return;
        }
        if (scope == BpmDataScope.DEPT) {
            List<Long> visible = securityGuard.visibleUserIds();
            if (visible == null || visible.isEmpty()) {
                // 组织数据不足时按本人兜底，避免越权展示
                wrapper.eq(BpmTask::getAssigneeId, current);
                return;
            }
            wrapper.in(BpmTask::getAssigneeId, visible);
            return;
        }
        wrapper.eq(BpmTask::getAssigneeId, current);
    }

    /** 抄送数据范围：ALL 不过滤；DEPT 同部门及子部门用户；SELF 仅本人。 */
    private void applyReceiverScope(LambdaQueryWrapper<BpmCcRecord> wrapper, Long current) {
        BpmDataScope scope = securityGuard.resolveDataScope();
        if (scope == BpmDataScope.ALL) {
            return;
        }
        if (scope == BpmDataScope.DEPT) {
            List<Long> visible = securityGuard.visibleUserIds();
            if (visible == null || visible.isEmpty()) {
                wrapper.eq(BpmCcRecord::getReceiverId, current);
                return;
            }
            wrapper.in(BpmCcRecord::getReceiverId, visible);
            return;
        }
        wrapper.eq(BpmCcRecord::getReceiverId, current);
    }
}
