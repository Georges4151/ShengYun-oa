package com.ceams.module.bpm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.common.response.PageResult;
import com.ceams.module.bpm.dto.InstanceActionRequest;
import com.ceams.module.bpm.dto.InstanceStartRequest;
import com.ceams.module.bpm.entity.BpmCcRecord;
import com.ceams.module.bpm.entity.BpmInstance;
import com.ceams.module.bpm.entity.BpmNode;
import com.ceams.module.bpm.entity.BpmTask;
import com.ceams.module.bpm.entity.BpmTemplate;
import com.ceams.module.bpm.entity.BpmTemplateVersion;
import com.ceams.module.bpm.entity.BpmVariable;
import com.ceams.module.bpm.enums.AuditAction;
import com.ceams.module.bpm.enums.BpmBizType;
import com.ceams.module.bpm.enums.BpmDataScope;
import com.ceams.module.bpm.enums.BpmInstanceStatus;
import com.ceams.module.bpm.enums.BpmNodeStatus;
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
import com.ceams.module.bpm.service.BpmInstanceService;
import com.ceams.module.bpm.support.BpmFlowEngine;
import com.ceams.module.bpm.support.BpmSchema;
import com.ceams.module.bpm.support.BpmSchemaNode;
import com.ceams.module.bpm.support.BpmSchemaParser;
import com.ceams.module.bpm.support.BpmSecurityGuard;
import com.ceams.module.bpm.support.BpmViewAssembler;
import com.ceams.module.bpm.vo.InstanceDetailVO;
import com.ceams.module.bpm.vo.InstanceVO;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** 流程实例服务实现。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BpmInstanceServiceImpl implements BpmInstanceService {

    private final BpmInstanceMapper instanceMapper;
    private final BpmTemplateMapper templateMapper;
    private final BpmTemplateVersionMapper versionMapper;
    private final BpmNodeMapper nodeMapper;
    private final BpmTaskMapper taskMapper;
    private final BpmVariableMapper variableMapper;
    private final BpmCcRecordMapper ccRecordMapper;
    private final BpmSchemaParser schemaParser;
    private final BpmFlowEngine flowEngine;
    private final BpmAuditService auditService;
    private final BpmSecurityGuard securityGuard;
    private final BpmViewAssembler assembler;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String start(InstanceStartRequest request) {
        BpmTemplate template =
                templateMapper.selectOne(
                        new LambdaQueryWrapper<BpmTemplate>()
                                .eq(BpmTemplate::getCode, request.getTemplateCode()));
        if (template == null) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "流程模板不存在");
        }
        if (template.getStatus() != TemplateStatus.ENABLED || template.getCurrentVersionId() == null) {
            throw BusinessException.of(ErrorCode.BPM_TEMPLATE_NOT_ENABLED, "流程模板未启用，不可发起");
        }
        BpmTemplateVersion version = versionMapper.selectById(template.getCurrentVersionId());
        if (version == null || version.getStatus() != TemplateVersionStatus.PUBLISHED) {
            throw BusinessException.of(ErrorCode.BPM_TEMPLATE_NOT_ENABLED, "流程模板当前版本未发布");
        }
        BpmSchema schema = schemaParser.parse(version.getSchemaJson());

        Long initiatorId = securityGuard.currentUserId();
        if (initiatorId == null) {
            throw BusinessException.of(ErrorCode.AUTH_ACCESS_DENIED, "未登录不可发起流程");
        }
        String bizType =
                StringUtils.hasText(request.getBizType()) ? request.getBizType() : template.getBizType();
        // 业务类型必须与模板一致，避免通过传入任意 bizType 绕过发起权限
        if (!Objects.equals(bizType, template.getBizType()) || BpmBizType.from(bizType) == null) {
            throw BusinessException.of(ErrorCode.VALID_ILLEGAL_ARGUMENT, "业务类型与模板不一致或不受支持：" + bizType);
        }
        securityGuard.requireBizPermission(bizType);

        BpmInstance instance = new BpmInstance();
        instance.setTemplateVersionId(version.getId());
        instance.setBizType(bizType);
        instance.setBizId(request.getBizId());
        instance.setInitiatorId(initiatorId);
        instance.setStatus(BpmInstanceStatus.RUNNING);
        instance.setStartedAt(LocalDateTime.now());
        instanceMapper.insert(instance);

        List<BpmNode> nodes = createNodes(instance, schema);
        saveVariables(instance, template, request.getVariables());

        flowEngine.start(instance, nodes, schema);
        auditService.record(
                BpmAuditService.TARGET_INSTANCE,
                instance.getId(),
                AuditAction.START,
                null,
                instance,
                null,
                initiatorId);
        log.info(
                "发起 BPM 实例：instanceId={}，templateCode={}，bizType={}，initiator={}",
                instance.getId(),
                template.getCode(),
                instance.getBizType(),
                initiatorId);
        return String.valueOf(instance.getId());
    }

    @Override
    public InstanceDetailVO detail(Long id) {
        BpmInstance instance = requireInstance(id);
        assertVisible(instance);
        List<BpmNode> nodes = flowEngine.loadNodes(id);
        List<BpmTask> tasks =
                taskMapper.selectList(new LambdaQueryWrapper<BpmTask>().eq(BpmTask::getInstanceId, id));
        Map<String, String> variables =
                variableMapper
                        .selectList(
                                new LambdaQueryWrapper<BpmVariable>()
                                        .eq(BpmVariable::getInstanceId, id))
                        .stream()
                        .collect(
                                Collectors.toMap(
                                        BpmVariable::getVarKey,
                                        v -> Objects.toString(v.getVarValue(), ""),
                                        (a, b) -> a,
                                        LinkedHashMap::new));
        List<BpmCcRecord> ccRecords =
                ccRecordMapper.selectList(
                        new LambdaQueryWrapper<BpmCcRecord>().eq(BpmCcRecord::getInstanceId, id));

        InstanceDetailVO detail = new InstanceDetailVO();
        detail.setInstance(assembler.toInstanceVO(instance, nodes, tasks));
        detail.setNodes(assembler.toNodeVOs(nodes, tasks));
        detail.setVariables(variables);
        detail.setCcRecords(
                ccRecords.stream()
                        .map(
                                r ->
                                        assembler.toCcRecordVO(
                                                r,
                                                Map.of(instance.getId(), instance),
                                                assembler.index(nodes, BpmNode::getId)))
                        .toList());
        return detail;
    }

    @Override
    public PageResult<InstanceVO> pageByInitiator(
            long page, long size, BpmInstanceStatus status, String bizType) {
        Long initiatorId = securityGuard.currentUserId();
        if (initiatorId == null) {
            throw BusinessException.of(ErrorCode.AUTH_ACCESS_DENIED, "未登录不可查询");
        }
        Page<BpmInstance> p = new Page<>(Math.max(page, 1), Math.min(Math.max(size, 1), 100));
        LambdaQueryWrapper<BpmInstance> wrapper = new LambdaQueryWrapper<>();
        // 数据范围：SELF=仅本人；DEPT=同部门及子部门用户；ALL=管理员不限制
        applyInitiatorScope(wrapper, initiatorId);
        wrapper.eq(status != null, BpmInstance::getStatus, status);
        wrapper.eq(StringUtils.hasText(bizType), BpmInstance::getBizType, bizType);
        wrapper.orderByDesc(BpmInstance::getCreateTime);
        IPage<BpmInstance> result = instanceMapper.selectPage(p, wrapper);
        List<BpmInstance> records = result.getRecords();
        if (records.isEmpty()) {
            return new PageResult<>(List.of(), result.getTotal(), result.getCurrent(), result.getSize());
        }
        List<Long> ids = records.stream().map(BpmInstance::getId).toList();
        Map<Long, List<BpmNode>> nodeMap =
                nodeMapper
                        .selectList(
                                new LambdaQueryWrapper<BpmNode>()
                                        .in(BpmNode::getInstanceId, ids)
                                        .orderByAsc(BpmNode::getSeqNo))
                        .stream()
                        .collect(Collectors.groupingBy(BpmNode::getInstanceId));
        Map<Long, List<BpmTask>> taskMap =
                taskMapper
                        .selectList(new LambdaQueryWrapper<BpmTask>().in(BpmTask::getInstanceId, ids))
                        .stream()
                        .collect(Collectors.groupingBy(BpmTask::getInstanceId));
        List<InstanceVO> vos =
                records.stream()
                        .map(
                                i ->
                                        assembler.toInstanceVO(
                                                i,
                                                nodeMap.getOrDefault(i.getId(), List.of()),
                                                taskMap.getOrDefault(i.getId(), List.of())))
                        .toList();
        return new PageResult<>(vos, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void withdraw(Long id, InstanceActionRequest request) {
        BpmInstance instance = requireInstance(id);
        if (instance.getStatus() == BpmInstanceStatus.WITHDRAWN) {
            // 重复撤回：返回首次结果
            return;
        }
        if (instance.getStatus() != BpmInstanceStatus.RUNNING) {
            throw BusinessException.of(ErrorCode.BIZ_STATE_CONFLICT, "仅运行中实例可撤回");
        }
        Long current = securityGuard.currentUserId();
        if (current == null || !current.equals(instance.getInitiatorId())) {
            throw BusinessException.of(ErrorCode.AUTH_ACCESS_DENIED, "仅发起人可撤回");
        }
        requireInstanceVersion(instance, request.getInstanceVersion());
        if (flowEngine.hasProcessedTask(id)) {
            throw BusinessException.of(ErrorCode.BPM_INSTANCE_CONFLICT, "首节点已处理，不可撤回");
        }
        Map<String, Object> before = snapshotInstance(instance);
        List<BpmNode> nodes = flowEngine.loadNodes(id);
        flowEngine.withdraw(instance, nodes);
        auditService.record(
                BpmAuditService.TARGET_INSTANCE,
                id,
                AuditAction.WITHDRAW,
                before,
                instance,
                null,
                current);
        log.info("撤回 BPM 实例：instanceId={}，operator={}，reason={}", id, current, request.getReason());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void terminate(Long id, InstanceActionRequest request) {
        securityGuard.requireAdmin();
        BpmInstance instance = requireInstance(id);
        if (instance.getStatus() == BpmInstanceStatus.TERMINATED) {
            // 重复终止：返回首次结果
            return;
        }
        if (instance.getStatus() != BpmInstanceStatus.RUNNING) {
            throw BusinessException.of(ErrorCode.BIZ_STATE_CONFLICT, "仅运行中实例可终止");
        }
        requireInstanceVersion(instance, request.getInstanceVersion());
        Map<String, Object> before = snapshotInstance(instance);
        List<BpmNode> nodes = flowEngine.loadNodes(id);
        flowEngine.terminate(instance, nodes);
        auditService.record(
                BpmAuditService.TARGET_INSTANCE,
                id,
                AuditAction.TERMINATE,
                before,
                instance,
                null,
                securityGuard.currentUserId());
        log.info(
                "终止 BPM 实例：instanceId={}，operator={}，reason={}",
                id,
                securityGuard.currentUserId(),
                request.getReason());
    }

    private List<BpmNode> createNodes(BpmInstance instance, BpmSchema schema) {
        List<BpmNode> nodes = new ArrayList<>();
        for (BpmSchemaNode def : schema.getNodes()) {
            BpmNode node = new BpmNode();
            node.setInstanceId(instance.getId());
            node.setNodeKey(def.getNodeKey());
            node.setNodeType(def.getNodeType());
            node.setSeqNo(def.getSeqNo());
            node.setStatus(BpmNodeStatus.PENDING);
            nodeMapper.insert(node);
            nodes.add(node);
        }
        return nodes;
    }

    /** 写入流程变量：系统变量 biz_id/biz_type/template_code/initiator_id 优先，用户变量不得覆盖。 */
    private void saveVariables(
            BpmInstance instance, BpmTemplate template, Map<String, String> userVariables) {
        Map<String, String> variables = new LinkedHashMap<>();
        variables.put("biz_id", Objects.toString(instance.getBizId(), ""));
        variables.put("biz_type", instance.getBizType());
        variables.put("template_code", template.getCode());
        variables.put("initiator_id", String.valueOf(instance.getInitiatorId()));
        if (userVariables != null) {
            userVariables.forEach((key, value) -> {
                if (StringUtils.hasText(key)) {
                    variables.putIfAbsent(key, value);
                }
            });
        }
        variables.forEach(
                (key, value) -> {
                    BpmVariable variable = new BpmVariable();
                    variable.setInstanceId(instance.getId());
                    variable.setVarKey(key);
                    variable.setVarValue(value);
                    variableMapper.insert(variable);
                });
    }

    private BpmInstance requireInstance(Long id) {
        BpmInstance instance = instanceMapper.selectById(id);
        if (instance == null) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "流程实例不存在");
        }
        return instance;
    }

    private void requireInstanceVersion(BpmInstance instance, Integer requestVersion) {
        if (requestVersion == null || !requestVersion.equals(instance.getVersion())) {
            throw BusinessException.of(ErrorCode.BPM_INSTANCE_CONFLICT, "实例版本已变更，请刷新后重试");
        }
    }

    /** 详情可见性：管理员、发起人、任务处理人、抄送接收人才可查看。 */
    private void assertVisible(BpmInstance instance) {
        if (securityGuard.isAdmin()) {
            return;
        }
        Long current = securityGuard.currentUserId();
        if (current == null) {
            throw BusinessException.of(ErrorCode.AUTH_ACCESS_DENIED, "未登录不可查看");
        }
        if (current.equals(instance.getInitiatorId())) {
            return;
        }
        // DEPT 数据范围：同部门及子部门发起人的实例可见
        if (securityGuard.resolveDataScope() == BpmDataScope.DEPT) {
            List<Long> visible = securityGuard.visibleUserIds();
            if (visible != null && visible.contains(instance.getInitiatorId())) {
                return;
            }
        }
        long taskCount =
                taskMapper.selectCount(
                        new LambdaQueryWrapper<BpmTask>()
                                .eq(BpmTask::getInstanceId, instance.getId())
                                .eq(BpmTask::getAssigneeId, current));
        if (taskCount > 0) {
            return;
        }
        long ccCount =
                ccRecordMapper.selectCount(
                        new LambdaQueryWrapper<BpmCcRecord>()
                                .eq(BpmCcRecord::getInstanceId, instance.getId())
                                .eq(BpmCcRecord::getReceiverId, current));
        if (ccCount > 0) {
            return;
        }
        throw BusinessException.of(ErrorCode.AUTH_ACCESS_DENIED, "无权查看该流程实例");
    }

    /** 我发起的数据范围：ALL 不过滤；DEPT 同部门及子部门用户；SELF（含范围未知）仅本人。 */
    private void applyInitiatorScope(LambdaQueryWrapper<BpmInstance> wrapper, Long initiatorId) {
        BpmDataScope scope = securityGuard.resolveDataScope();
        if (scope == BpmDataScope.ALL) {
            return;
        }
        if (scope == BpmDataScope.DEPT) {
            List<Long> visible = securityGuard.visibleUserIds();
            if (visible == null || visible.isEmpty()) {
                wrapper.eq(BpmInstance::getInitiatorId, initiatorId);
                return;
            }
            wrapper.in(BpmInstance::getInitiatorId, visible);
            return;
        }
        wrapper.eq(BpmInstance::getInitiatorId, initiatorId);
    }

    private Map<String, Object> snapshotInstance(BpmInstance instance) {
        Map<String, Object> before = new LinkedHashMap<>();
        before.put("id", instance.getId());
        before.put("status", instance.getStatus());
        before.put("version", instance.getVersion());
        before.put("initiatorId", instance.getInitiatorId());
        before.put("bizType", instance.getBizType());
        before.put("bizId", instance.getBizId());
        before.put("endedAt", instance.getEndedAt());
        return before;
    }
}
