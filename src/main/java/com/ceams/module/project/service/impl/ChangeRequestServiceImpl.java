package com.ceams.module.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.common.response.PageResult;
import com.ceams.common.security.SecurityUtils;
import com.ceams.module.bpm.dto.InstanceStartRequest;
import com.ceams.module.bpm.enums.BpmBizType;
import com.ceams.module.bpm.service.BpmInstanceService;
import com.ceams.module.project.converter.ChangeRequestConverter;
import com.ceams.module.project.dto.changerequest.ChangeRequestCreateRequest;
import com.ceams.module.project.dto.changerequest.ChangeRequestUpdateRequest;
import com.ceams.module.project.entity.ChangeRequest;
import com.ceams.module.project.entity.Project;
import com.ceams.module.project.enums.ApprovalStatus;
import com.ceams.module.project.mapper.ChangeRequestMapper;
import com.ceams.module.project.mapper.ProjectMapper;
import com.ceams.module.project.service.ChangeRequestService;
import com.ceams.module.project.util.PageUtil;
import com.ceams.module.project.vo.ChangeRequestVO;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 变更请求服务实现。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChangeRequestServiceImpl implements ChangeRequestService {

    /** 项目变更默认流程模板编码。 */
    private static final String TEMPLATE_CODE = "CHANGE_REQUEST";

    private final ChangeRequestMapper changeRequestMapper;
    private final ProjectMapper projectMapper;
    private final ChangeRequestConverter changeRequestConverter;
    private final BpmInstanceService bpmInstanceService;

    @Override
    public PageResult<ChangeRequestVO> page(
            long page, long size, Long projectId, ApprovalStatus status) {
        Page<ChangeRequest> p = PageUtil.resolve(page, size);
        LambdaQueryWrapper<ChangeRequest> wrapper = new LambdaQueryWrapper<>();
        if (projectId != null) {
            wrapper.eq(ChangeRequest::getProjectId, projectId);
        }
        if (status != null) {
            wrapper.eq(ChangeRequest::getStatus, status);
        }
        wrapper.orderByDesc(ChangeRequest::getCreateTime);
        IPage<ChangeRequest> result = changeRequestMapper.selectPage(p, wrapper);
        List<ChangeRequestVO> records = changeRequestConverter.toVOList(result.getRecords());
        fillProjectNames(records);
        return new PageResult<>(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public ChangeRequestVO detail(Long id) {
        ChangeRequestVO vo = changeRequestConverter.toVO(requireChangeRequest(id));
        fillProjectNames(List.of(vo));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String create(ChangeRequestCreateRequest request) {
        requireProject(request.getProjectId());
        ChangeRequest changeRequest = new ChangeRequest();
        changeRequest.setProjectId(request.getProjectId());
        changeRequest.setRequestType(request.getRequestType());
        changeRequest.setDescription(request.getDescription());
        changeRequest.setEstimatedCost(request.getEstimatedCost());
        // 新建一律为草稿态，提交后才发起 BPM 流程（审批状态唯一来源是实例）
        changeRequest.setStatus(ApprovalStatus.DRAFT);
        changeRequest.setRequestedBy(SecurityUtils.getCurrentUserId());
        changeRequestMapper.insert(changeRequest);
        return String.valueOf(changeRequest.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, ChangeRequestUpdateRequest request) {
        ChangeRequest changeRequest = requireChangeRequest(id);
        ApprovalStatus status = changeRequest.getStatus();
        if (status != ApprovalStatus.DRAFT && status != ApprovalStatus.REJECTED) {
            throw BusinessException.of(ErrorCode.BIZ_STATE_CONFLICT, "仅草稿态或驳回态的变更请求可编辑");
        }
        if (StringUtils.isNotBlank(request.getRequestType())) {
            changeRequest.setRequestType(request.getRequestType());
        }
        if (request.getDescription() != null) {
            changeRequest.setDescription(request.getDescription());
        }
        if (request.getEstimatedCost() != null) {
            changeRequest.setEstimatedCost(request.getEstimatedCost());
        }
        changeRequestMapper.updateById(changeRequest);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        ChangeRequest changeRequest = requireChangeRequest(id);
        if (changeRequest.getStatus() != ApprovalStatus.DRAFT) {
            throw BusinessException.of(ErrorCode.BIZ_STATE_CONFLICT, "仅草稿态的变更请求可删除");
        }
        changeRequestMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submit(Long id) {
        ChangeRequest changeRequest = requireChangeRequest(id);
        ApprovalStatus status = changeRequest.getStatus();
        if (status != ApprovalStatus.DRAFT && status != ApprovalStatus.REJECTED) {
            throw BusinessException.of(ErrorCode.BIZ_STATE_CONFLICT, "仅草稿态或驳回态的变更请求可提交");
        }
        InstanceStartRequest request = new InstanceStartRequest();
        request.setTemplateCode(TEMPLATE_CODE);
        request.setBizType(BpmBizType.CHANGE_REQUEST.name());
        request.setBizId(changeRequest.getId());
        Map<String, String> variables = new LinkedHashMap<>();
        variables.put(
                "projectId",
                changeRequest.getProjectId() == null
                        ? ""
                        : String.valueOf(changeRequest.getProjectId()));
        variables.put(
                "estimatedCost",
                changeRequest.getEstimatedCost() == null
                        ? "0"
                        : changeRequest.getEstimatedCost().toPlainString());
        variables.put(
                "requestType",
                changeRequest.getRequestType() == null ? "" : changeRequest.getRequestType());
        request.setVariables(variables);

        String instanceId = bpmInstanceService.start(request);
        changeRequest.setBpmInstanceId(Long.valueOf(instanceId));
        changeRequest.setStatus(ApprovalStatus.PENDING);
        changeRequestMapper.updateById(changeRequest);
        log.info("变更请求 {} 已提交并发起 BPM 实例 {}", id, instanceId);
    }

    /** 批量回填项目名称，避免逐条查询造成 N+1。 */
    private void fillProjectNames(List<ChangeRequestVO> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        List<Long> projectIds =
                records.stream()
                        .map(ChangeRequestVO::getProjectId)
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList();
        if (projectIds.isEmpty()) {
            return;
        }
        Map<Long, String> projectNames = new HashMap<>();
        for (Project project : projectMapper.selectBatchIds(projectIds)) {
            projectNames.put(project.getId(), project.getProjectName());
        }
        for (ChangeRequestVO record : records) {
            if (record.getProjectId() != null) {
                record.setProjectName(projectNames.get(record.getProjectId()));
            }
        }
    }

    private ChangeRequest requireChangeRequest(Long id) {
        ChangeRequest changeRequest = changeRequestMapper.selectById(id);
        if (changeRequest == null) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "变更请求不存在");
        }
        return changeRequest;
    }

    private void requireProject(Long projectId) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "项目不存在");
        }
    }
}
