package com.ceams.module.bpm.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.module.bpm.dto.InstanceActionRequest;
import com.ceams.module.bpm.dto.InstanceStartRequest;
import com.ceams.module.bpm.entity.BpmInstance;
import com.ceams.module.bpm.entity.BpmTemplate;
import com.ceams.module.bpm.entity.BpmTemplateVersion;
import com.ceams.module.bpm.entity.BpmVariable;
import com.ceams.module.bpm.enums.BpmInstanceStatus;
import com.ceams.module.bpm.enums.BpmNodeType;
import com.ceams.module.bpm.enums.TemplateStatus;
import com.ceams.module.bpm.enums.TemplateVersionStatus;
import com.ceams.module.bpm.mapper.BpmCcRecordMapper;
import com.ceams.module.bpm.mapper.BpmInstanceMapper;
import com.ceams.module.bpm.mapper.BpmNodeMapper;
import com.ceams.module.bpm.mapper.BpmTaskMapper;
import com.ceams.module.bpm.mapper.BpmTemplateMapper;
import com.ceams.module.bpm.mapper.BpmTemplateVersionMapper;
import com.ceams.module.bpm.mapper.BpmVariableMapper;
import com.ceams.module.bpm.service.BpmAuditService;
import com.ceams.module.bpm.support.BpmFlowEngine;
import com.ceams.module.bpm.support.BpmSchema;
import com.ceams.module.bpm.support.BpmSchemaNode;
import com.ceams.module.bpm.support.BpmSchemaParser;
import com.ceams.module.bpm.support.BpmSecurityGuard;
import com.ceams.module.bpm.support.BpmViewAssembler;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** 流程实例服务单元测试：发起校验、撤回/终止权限与状态、幂等。 */
@ExtendWith(MockitoExtension.class)
class BpmInstanceServiceImplTest {

    @Mock private BpmInstanceMapper instanceMapper;
    @Mock private BpmTemplateMapper templateMapper;
    @Mock private BpmTemplateVersionMapper versionMapper;
    @Mock private BpmNodeMapper nodeMapper;
    @Mock private BpmTaskMapper taskMapper;
    @Mock private BpmVariableMapper variableMapper;
    @Mock private BpmCcRecordMapper ccRecordMapper;
    @Mock private BpmSchemaParser schemaParser;
    @Mock private BpmFlowEngine flowEngine;
    @Mock private BpmAuditService auditService;
    @Mock private BpmSecurityGuard securityGuard;
    @Mock private BpmViewAssembler assembler;

    private BpmInstanceServiceImpl service;

    @BeforeEach
    void setUp() {
        service =
                new BpmInstanceServiceImpl(
                        instanceMapper,
                        templateMapper,
                        versionMapper,
                        nodeMapper,
                        taskMapper,
                        variableMapper,
                        ccRecordMapper,
                        schemaParser,
                        flowEngine,
                        auditService,
                        securityGuard,
                        assembler);
        lenient().when(securityGuard.currentUserId()).thenReturn(10L);
    }

    @Test
    void 模板未启用时拒绝发起() {
        BpmTemplate template = template(TemplateStatus.DISABLED, null);
        when(templateMapper.selectOne(any(Wrapper.class))).thenReturn(template);

        assertThatThrownBy(() -> service.start(startRequest()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.BPM_TEMPLATE_NOT_ENABLED);
        verify(instanceMapper, never()).insert(any(BpmInstance.class));
    }

    @Test
    void 当前版本未发布时拒绝发起() {
        BpmTemplate template = template(TemplateStatus.ENABLED, 20L);
        when(templateMapper.selectOne(any(Wrapper.class))).thenReturn(template);
        BpmTemplateVersion version = version(TemplateVersionStatus.ARCHIVED);
        when(versionMapper.selectById(20L)).thenReturn(version);

        assertThatThrownBy(() -> service.start(startRequest()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.BPM_TEMPLATE_NOT_ENABLED);
    }

    @Test
    void 发起成功写入实例节点变量并调用流程引擎() {
        BpmTemplate template = template(TemplateStatus.ENABLED, 20L);
        when(templateMapper.selectOne(any(Wrapper.class))).thenReturn(template);
        when(versionMapper.selectById(20L)).thenReturn(version(TemplateVersionStatus.PUBLISHED));
        when(schemaParser.parse(any())).thenReturn(schema());
        doAnswer(
                        invocation -> {
                            BpmInstance instance = invocation.getArgument(0);
                            instance.setId(99L);
                            return 1;
                        })
                .when(instanceMapper)
                .insert(any(BpmInstance.class));

        InstanceStartRequest request = startRequest();
        request.setVariables(Map.of("amount", "1000"));
        String id = service.start(request);

        assertThat(id).isEqualTo("99");
        verify(nodeMapper, org.mockito.Mockito.times(2)).insert(any(com.ceams.module.bpm.entity.BpmNode.class));
        // 系统变量 biz_id/biz_type/template_code/initiator_id + 用户变量 amount
        verify(variableMapper, org.mockito.Mockito.times(5)).insert(any(BpmVariable.class));
        verify(flowEngine).start(any(BpmInstance.class), any(), any());
        verify(auditService).record(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void 无业务发起权限时拒绝发起() {
        BpmTemplate template = template(TemplateStatus.ENABLED, 20L);
        when(templateMapper.selectOne(any(Wrapper.class))).thenReturn(template);
        when(versionMapper.selectById(20L)).thenReturn(version(TemplateVersionStatus.PUBLISHED));
        when(schemaParser.parse(any())).thenReturn(schema());
        doThrow(new BusinessException(ErrorCode.AUTH_ACCESS_DENIED))
                .when(securityGuard)
                .requireBizPermission("EXPENSE");

        assertThatThrownBy(() -> service.start(startRequest()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.AUTH_ACCESS_DENIED);
        verify(instanceMapper, never()).insert(any(BpmInstance.class));
    }

    @Test
    void 非发起人撤回被拒绝() {
        BpmInstance instance = instance(1L, 10L, BpmInstanceStatus.RUNNING, 2);
        when(instanceMapper.selectById(1L)).thenReturn(instance);
        lenient().when(securityGuard.currentUserId()).thenReturn(5L);
        InstanceActionRequest request = actionRequest(2);

        assertThatThrownBy(() -> service.withdraw(1L, request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.AUTH_ACCESS_DENIED);
        verify(flowEngine, never()).withdraw(any(), any());
    }

    @Test
    void 首节点已处理时不可撤回() {
        BpmInstance instance = instance(1L, 10L, BpmInstanceStatus.RUNNING, 2);
        when(instanceMapper.selectById(1L)).thenReturn(instance);
        when(flowEngine.hasProcessedTask(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.withdraw(1L, actionRequest(2)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.BPM_INSTANCE_CONFLICT);
    }

    @Test
    void 发起人撤回成功并写审计() {
        BpmInstance instance = instance(1L, 10L, BpmInstanceStatus.RUNNING, 2);
        when(instanceMapper.selectById(1L)).thenReturn(instance);
        when(flowEngine.hasProcessedTask(1L)).thenReturn(false);

        service.withdraw(1L, actionRequest(2));

        verify(flowEngine).withdraw(any(BpmInstance.class), any());
        verify(auditService).record(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void 重复撤回返回首次结果() {
        BpmInstance instance = instance(1L, 10L, BpmInstanceStatus.WITHDRAWN, 5);
        when(instanceMapper.selectById(1L)).thenReturn(instance);

        service.withdraw(1L, actionRequest(1));

        verify(flowEngine, never()).withdraw(any(), any());
    }

    @Test
    void 非管理员终止被拒绝() {
        doThrow(new BusinessException(ErrorCode.AUTH_ACCESS_DENIED))
                .when(securityGuard)
                .requireAdmin();

        assertThatThrownBy(() -> service.terminate(1L, actionRequest(2)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.AUTH_ACCESS_DENIED);
    }

    @Test
    void 管理员终止成功() {
        lenient().when(securityGuard.isAdmin()).thenReturn(true);
        BpmInstance instance = instance(1L, 10L, BpmInstanceStatus.RUNNING, 2);
        when(instanceMapper.selectById(1L)).thenReturn(instance);

        service.terminate(1L, actionRequest(2));

        verify(flowEngine).terminate(any(BpmInstance.class), any());
        verify(auditService).record(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void 非运行中实例不可终止() {
        BpmInstance instance = instance(1L, 10L, BpmInstanceStatus.APPROVED, 2);
        when(instanceMapper.selectById(1L)).thenReturn(instance);

        assertThatThrownBy(() -> service.terminate(1L, actionRequest(2)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.BIZ_STATE_CONFLICT);
    }

    @Test
    void 无权查看他人实例详情被拒绝() {
        BpmInstance instance = instance(1L, 10L, BpmInstanceStatus.RUNNING, 2);
        when(instanceMapper.selectById(1L)).thenReturn(instance);
        lenient().when(securityGuard.currentUserId()).thenReturn(5L);
        lenient().when(securityGuard.isAdmin()).thenReturn(false);
        when(taskMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(ccRecordMapper.selectCount(any(Wrapper.class))).thenReturn(0L);

        assertThatThrownBy(() -> service.detail(1L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.AUTH_ACCESS_DENIED);
    }

    // ------------------------------------------------------------------
    // 夹具
    // ------------------------------------------------------------------

    private BpmTemplate template(TemplateStatus status, Long currentVersionId) {
        BpmTemplate template = new BpmTemplate();
        template.setId(1L);
        template.setCode("EXPENSE_FLOW");
        template.setName("报销审批");
        template.setBizType("EXPENSE");
        template.setStatus(status);
        template.setCurrentVersionId(currentVersionId);
        return template;
    }

    private BpmTemplateVersion version(TemplateVersionStatus status) {
        BpmTemplateVersion version = new BpmTemplateVersion();
        version.setId(20L);
        version.setTemplateId(1L);
        version.setVersionNo(1);
        version.setStatus(status);
        version.setSchemaJson("{}");
        return version;
    }

    private BpmSchema schema() {
        BpmSchema schema = new BpmSchema();
        BpmSchemaNode start = new BpmSchemaNode();
        start.setNodeKey("start");
        start.setSeqNo(1);
        start.setNodeType(BpmNodeType.START);
        BpmSchemaNode approve = new BpmSchemaNode();
        approve.setNodeKey("approve");
        approve.setSeqNo(2);
        approve.setNodeType(BpmNodeType.SINGLE);
        schema.setNodes(List.of(start, approve));
        return schema;
    }

    private InstanceStartRequest startRequest() {
        InstanceStartRequest request = new InstanceStartRequest();
        request.setTemplateCode("EXPENSE_FLOW");
        request.setBizId(100L);
        return request;
    }

    private InstanceActionRequest actionRequest(int version) {
        InstanceActionRequest request = new InstanceActionRequest();
        request.setInstanceVersion(version);
        request.setReason("测试");
        return request;
    }

    private BpmInstance instance(
            Long id, Long initiator, BpmInstanceStatus status, int version) {
        BpmInstance instance = new BpmInstance();
        instance.setId(id);
        instance.setInitiatorId(initiator);
        instance.setStatus(status);
        instance.setVersion(version);
        instance.setTemplateVersionId(20L);
        instance.setBizType("EXPENSE");
        return instance;
    }
}
