package com.ceams.module.bpm.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.module.bpm.dto.TaskApproveRequest;
import com.ceams.module.bpm.dto.TaskDelegateRequest;
import com.ceams.module.bpm.dto.TaskRejectRequest;
import com.ceams.module.bpm.entity.BpmInstance;
import com.ceams.module.bpm.entity.BpmNode;
import com.ceams.module.bpm.entity.BpmTask;
import com.ceams.module.bpm.entity.BpmTemplateVersion;
import com.ceams.module.bpm.enums.BpmInstanceStatus;
import com.ceams.module.bpm.enums.BpmNodeStatus;
import com.ceams.module.bpm.enums.BpmNodeType;
import com.ceams.module.bpm.enums.RejectTarget;
import com.ceams.module.bpm.enums.BpmTaskStatus;
import com.ceams.module.bpm.mapper.BpmCcRecordMapper;
import com.ceams.module.bpm.mapper.BpmInstanceMapper;
import com.ceams.module.bpm.mapper.BpmNodeMapper;
import com.ceams.module.bpm.mapper.BpmTaskMapper;
import com.ceams.module.bpm.mapper.BpmTemplateVersionMapper;
import com.ceams.module.bpm.service.BpmAuditService;
import com.ceams.module.bpm.service.BpmInstanceService;
import com.ceams.module.bpm.support.BpmFlowEngine;
import com.ceams.module.bpm.support.BpmNotificationPublisher;
import com.ceams.module.bpm.support.BpmSchema;
import com.ceams.module.bpm.support.BpmSchemaParser;
import com.ceams.module.bpm.support.BpmSecurityGuard;
import com.ceams.module.bpm.support.BpmViewAssembler;
import com.ceams.module.bpm.vo.TaskVO;
import com.ceams.module.iam.entity.SysUser;
import com.ceams.module.iam.enums.UserStatus;
import com.ceams.module.iam.mapper.UserMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** 审批任务服务单元测试：会签/或签、驳回、幂等、乐观锁冲突、越权与改派。 */
@ExtendWith(MockitoExtension.class)
class BpmTaskServiceImplTest {

    @Mock private BpmTaskMapper taskMapper;
    @Mock private BpmNodeMapper nodeMapper;
    @Mock private BpmInstanceMapper instanceMapper;
    @Mock private BpmTemplateVersionMapper versionMapper;
    @Mock private BpmCcRecordMapper ccRecordMapper;
    @Mock private UserMapper userMapper;
    @Mock private BpmSchemaParser schemaParser;
    @Mock private BpmFlowEngine flowEngine;
    @Mock private BpmAuditService auditService;
    @Mock private BpmSecurityGuard securityGuard;
    @Mock private BpmViewAssembler assembler;
    @Mock private BpmInstanceService instanceService;
    @Mock private BpmNotificationPublisher notificationPublisher;

    private BpmTaskServiceImpl service;

    @BeforeEach
    void setUp() {
        service =
                new BpmTaskServiceImpl(
                        taskMapper,
                        nodeMapper,
                        instanceMapper,
                        versionMapper,
                        ccRecordMapper,
                        userMapper,
                        schemaParser,
                        flowEngine,
                        auditService,
                        securityGuard,
                        assembler,
                        instanceService,
                        notificationPublisher);
        lenient().when(securityGuard.currentUserId()).thenReturn(5L);
        lenient().when(taskMapper.updateById(any(BpmTask.class))).thenReturn(1);
        lenient().when(taskMapper.insert(any(BpmTask.class))).thenReturn(1);
    }

    @Test
    void 会签首人同意节点保持活动不流转() {
        BpmTask task = task(1L, 1L, BpmTaskStatus.PENDING, 1);
        BpmInstance instance = instance(2L, 2);
        BpmNode node = node(BpmNodeType.COUNTERSIGN);
        stubTask(task, instance, node);
        lenient().when(taskMapper.selectCount(any(Wrapper.class))).thenReturn(1L);

        service.approve(1L, approveRequest(1, 2));

        assertThat(task.getStatus()).isEqualTo(BpmTaskStatus.APPROVED);
        verify(flowEngine, never()).afterApprove(any(), any(), any(), any());
    }

    @Test
    void 会签最后一人同意才流转到下一节点() {
        BpmTask task = task(1L, 1L, BpmTaskStatus.PENDING, 1);
        BpmInstance instance = instance(2L, 2);
        BpmNode node = node(BpmNodeType.COUNTERSIGN);
        stubTask(task, instance, node);
        stubFlow(instance, node);
        lenient().when(taskMapper.selectCount(any(Wrapper.class))).thenReturn(0L);

        service.approve(1L, approveRequest(1, 2));

        verify(flowEngine).afterApprove(any(), any(), any(), any());
    }

    @Test
    void 或签任何人同意即关闭其他待办并流转() {
        BpmTask task = task(1L, 1L, BpmTaskStatus.PENDING, 1);
        BpmInstance instance = instance(2L, 2);
        BpmNode node = node(BpmNodeType.OR_SIGN);
        stubTask(task, instance, node);
        stubFlow(instance, node);

        service.approve(1L, approveRequest(1, 2));

        verify(taskMapper).update(any(BpmTask.class), any(Wrapper.class));
        verify(flowEngine).afterApprove(any(), any(), any(), any());
    }

    @Test
    void 或签任一驳回即整体驳回并关闭其他待办() {
        BpmTask task = task(1L, 1L, BpmTaskStatus.PENDING, 1);
        BpmInstance instance = instance(2L, 2);
        BpmNode node = node(BpmNodeType.OR_SIGN);
        stubTask(task, instance, node);
        stubFlow(instance, node);
        // 同节点仍有其他待办：或签任一人驳回即出结果，不再要求全员驳回
        lenient().when(taskMapper.selectCount(any(Wrapper.class))).thenReturn(1L);

        service.reject(1L, rejectRequest(RejectTarget.INITIATOR));

        assertThat(task.getStatus()).isEqualTo(BpmTaskStatus.REJECTED);
        verify(taskMapper).update(any(BpmTask.class), any(Wrapper.class));
        verify(flowEngine).afterReject(any(), any(), any(), any(), any());
    }

    @Test
    void 会签任一驳回整体驳回并关闭其他待办() {
        BpmTask task = task(1L, 1L, BpmTaskStatus.PENDING, 1);
        BpmInstance instance = instance(2L, 2);
        BpmNode node = node(BpmNodeType.COUNTERSIGN);
        stubTask(task, instance, node);
        stubFlow(instance, node);

        service.reject(1L, rejectRequest(RejectTarget.INITIATOR));

        verify(taskMapper).update(any(BpmTask.class), any(Wrapper.class));
        verify(flowEngine).afterReject(any(), any(), any(), any(), any());
    }

    @Test
    void 同一任务重复同意返回首次结果() {
        BpmTask task = task(1L, 1L, BpmTaskStatus.APPROVED, 5);
        lenient().when(taskMapper.selectById(1L)).thenReturn(task);

        service.approve(1L, approveRequest(3, 9));

        verify(taskMapper, never()).updateById(any(BpmTask.class));
        verify(flowEngine, never()).afterApprove(any(), any(), any(), any());
    }

    @Test
    void 同一任务重复驳回返回首次结果() {
        BpmTask task = task(1L, 1L, BpmTaskStatus.REJECTED, 5);
        lenient().when(taskMapper.selectById(1L)).thenReturn(task);

        service.reject(1L, rejectRequest(RejectTarget.INITIATOR));

        verify(taskMapper, never()).updateById(any(BpmTask.class));
        verify(flowEngine, never()).afterReject(any(), any(), any(), any(), any());
    }

    @Test
    void 乐观锁更新失败返回任务冲突错误码() {
        BpmTask task = task(1L, 1L, BpmTaskStatus.PENDING, 1);
        BpmInstance instance = instance(2L, 2);
        BpmNode node = node(BpmNodeType.SINGLE);
        stubTask(task, instance, node);
        when(taskMapper.updateById(any(BpmTask.class))).thenReturn(0);

        assertThatThrownBy(() -> service.approve(1L, approveRequest(1, 2)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.BPM_TASK_CONFLICT);
    }

    @Test
    void 非本人待办拒绝处理() {
        BpmTask task = task(1L, 1L, BpmTaskStatus.PENDING, 1);
        lenient().when(taskMapper.selectById(1L)).thenReturn(task);
        doThrow(new BusinessException(ErrorCode.BPM_TASK_NOT_OWNED))
                .when(securityGuard)
                .requireTaskOwner(anyLong());

        assertThatThrownBy(() -> service.approve(1L, approveRequest(1, 2)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.BPM_TASK_NOT_OWNED);
        verify(taskMapper, never()).updateById(any(BpmTask.class));
    }

    @Test
    void 任务版本不一致返回任务冲突() {
        BpmTask task = task(1L, 1L, BpmTaskStatus.PENDING, 1);
        BpmInstance instance = instance(2L, 2);
        stubTask(task, instance, node(BpmNodeType.SINGLE));

        assertThatThrownBy(() -> service.approve(1L, approveRequest(2, 2)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.BPM_TASK_CONFLICT);
    }

    @Test
    void 实例版本不一致返回实例冲突() {
        BpmTask task = task(1L, 1L, BpmTaskStatus.PENDING, 1);
        BpmInstance instance = instance(2L, 2);
        stubTask(task, instance, node(BpmNodeType.SINGLE));

        assertThatThrownBy(() -> service.approve(1L, approveRequest(1, 3)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.BPM_INSTANCE_CONFLICT);
    }

    @Test
    void 管理员改派关闭旧任务并生成新任务() {
        BpmTask task = task(1L, 1L, BpmTaskStatus.PENDING, 1);
        BpmInstance instance = instance(2L, 2);
        BpmNode node = node(BpmNodeType.SINGLE);
        stubTask(task, instance, node);
        SysUser target = new SysUser();
        target.setId(88L);
        target.setStatus(UserStatus.ACTIVE);
        when(userMapper.selectById(88L)).thenReturn(target);

        TaskDelegateRequest request = new TaskDelegateRequest();
        request.setTaskVersion(1);
        request.setInstanceVersion(2);
        request.setTargetUserId(88L);
        service.delegate(1L, request);

        assertThat(task.getStatus()).isEqualTo(BpmTaskStatus.CLOSED);
        verify(taskMapper).insert(any(BpmTask.class));
        verify(flowEngine).recordDelegateSnapshot(any(), org.mockito.ArgumentMatchers.eq(88L));
    }

    @Test
    void 无改派权限被拒绝() {
        doThrow(new BusinessException(ErrorCode.AUTH_ACCESS_DENIED))
                .when(securityGuard)
                .requireAdminOrPermission("bpm:task:delegate");

        TaskDelegateRequest request = new TaskDelegateRequest();
        request.setTaskVersion(1);
        request.setInstanceVersion(2);
        request.setTargetUserId(88L);

        assertThatThrownBy(() -> service.delegate(1L, request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.AUTH_ACCESS_DENIED);
        verify(taskMapper, never()).selectById(anyLong());
    }

    @Test
    void 具备改派权限的非管理员可以改派() {
        // 服务层不再强依赖管理员，具备 bpm:task:delegate 权限即可（守卫内部判定）
        BpmTask task = task(1L, 1L, BpmTaskStatus.PENDING, 1);
        BpmInstance instance = instance(2L, 2);
        BpmNode node = node(BpmNodeType.SINGLE);
        stubTask(task, instance, node);
        SysUser target = new SysUser();
        target.setId(66L);
        target.setStatus(UserStatus.ACTIVE);
        when(userMapper.selectById(66L)).thenReturn(target);

        TaskDelegateRequest request = new TaskDelegateRequest();
        request.setTaskVersion(1);
        request.setTargetUserId(66L);
        service.delegate(1L, request);

        verify(securityGuard).requireAdminOrPermission("bpm:task:delegate");
        verify(taskMapper).insert(any(BpmTask.class));
    }

    @Test
    void 我的待办分页返回统一视图() {
        BpmTask task = task(1L, 1L, BpmTaskStatus.PENDING, 1);
        Page<BpmTask> page = new Page<>(1, 20);
        page.setRecords(List.of(task));
        page.setTotal(1);
        when(taskMapper.selectPage(any(Page.class), any(Wrapper.class))).thenReturn(page);
        BpmInstance instance = instance(2L, 2);
        when(instanceMapper.selectBatchIds(any())).thenReturn(List.of(instance));
        BpmNode node = node(BpmNodeType.SINGLE);
        when(nodeMapper.selectBatchIds(any())).thenReturn(List.of(node));
        TaskVO vo = new TaskVO();
        vo.setId(1L);
        when(assembler.toTaskVO(any(BpmTask.class), any(), any())).thenReturn(vo);

        var result = service.pagePending(1, 20, null, false);

        assertThat(result.getRecords()).hasSize(1);
        assertThat(result.getTotal()).isEqualTo(1);
    }

    // ------------------------------------------------------------------
    // 夹具
    // ------------------------------------------------------------------

    private BpmTask task(Long id, Long instanceId, BpmTaskStatus status, int version) {
        BpmTask task = new BpmTask();
        task.setId(id);
        task.setInstanceId(instanceId);
        task.setNodeId(11L);
        task.setAssigneeId(5L);
        task.setStatus(status);
        task.setVersion(version);
        return task;
    }

    private BpmInstance instance(Long id, int version) {
        BpmInstance instance = new BpmInstance();
        instance.setId(id);
        instance.setInitiatorId(10L);
        instance.setStatus(BpmInstanceStatus.RUNNING);
        instance.setVersion(version);
        instance.setTemplateVersionId(20L);
        return instance;
    }

    private BpmNode node(BpmNodeType type) {
        BpmNode node = new BpmNode();
        node.setId(11L);
        node.setInstanceId(1L);
        node.setNodeKey("approve");
        node.setNodeType(type);
        node.setSeqNo(1);
        node.setStatus(BpmNodeStatus.ACTIVE);
        node.setVersion(0);
        return node;
    }

    private void stubTask(BpmTask task, BpmInstance instance, BpmNode node) {
        // 按任务自身的 instanceId/nodeId 打桩，避免夹具 id 不一致导致“实例不存在”
        lenient().when(taskMapper.selectById(task.getId())).thenReturn(task);
        lenient().when(instanceMapper.selectById(task.getInstanceId())).thenReturn(instance);
        lenient().when(nodeMapper.selectById(task.getNodeId())).thenReturn(node);
    }

    private void stubFlow(BpmInstance instance, BpmNode node) {
        BpmTemplateVersion version = new BpmTemplateVersion();
        version.setId(20L);
        version.setSchemaJson("{}");
        lenient().when(versionMapper.selectById(20L)).thenReturn(version);
        lenient().when(schemaParser.parse(anyString())).thenReturn(new BpmSchema());
        lenient().when(flowEngine.loadNodes(instance.getId())).thenReturn(List.of(node));
    }

    private TaskApproveRequest approveRequest(int taskVersion, int instanceVersion) {
        TaskApproveRequest request = new TaskApproveRequest();
        request.setTaskVersion(taskVersion);
        request.setInstanceVersion(instanceVersion);
        request.setComment("同意");
        return request;
    }

    private TaskRejectRequest rejectRequest(RejectTarget target) {
        TaskRejectRequest request = new TaskRejectRequest();
        request.setTaskVersion(1);
        request.setInstanceVersion(2);
        request.setTarget(target);
        request.setReason("资料不全");
        return request;
    }
}
