package com.ceams.module.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.common.response.PageResult;
import com.ceams.common.security.SecurityUtils;
import com.ceams.module.project.converter.ChangeRequestConverter;
import com.ceams.module.project.dto.changerequest.ApprovalActionRequest;
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
import java.time.LocalDateTime;
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

    private final ChangeRequestMapper changeRequestMapper;
    private final ProjectMapper projectMapper;
    private final ChangeRequestConverter changeRequestConverter;

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
        return new PageResult<>(
                changeRequestConverter.toVOList(result.getRecords()),
                result.getTotal(),
                result.getCurrent(),
                result.getSize());
    }

    @Override
    public ChangeRequestVO detail(Long id) {
        return changeRequestConverter.toVO(requireChangeRequest(id));
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
        changeRequest.setStatus(
                request.getStatus() == null ? ApprovalStatus.PENDING : request.getStatus());
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
    public void approve(Long id, ApprovalActionRequest request) {
        ChangeRequest changeRequest = requireChangeRequest(id);
        if (changeRequest.getStatus() != ApprovalStatus.PENDING) {
            throw BusinessException.of(ErrorCode.BIZ_STATE_CONFLICT, "仅待审批状态的变更请求可审批");
        }
        changeRequest.setStatus(ApprovalStatus.APPROVED);
        changeRequest.setApprovedBy(SecurityUtils.getCurrentUserId());
        changeRequest.setApprovedAt(LocalDateTime.now());
        changeRequest.setApprovalComment(request == null ? null : request.getComment());
        changeRequestMapper.updateById(changeRequest);
        log.info("变更请求 {} 已审批通过，意见：{}", id, request == null ? null : request.getComment());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(Long id, ApprovalActionRequest request) {
        ChangeRequest changeRequest = requireChangeRequest(id);
        if (changeRequest.getStatus() != ApprovalStatus.PENDING) {
            throw BusinessException.of(ErrorCode.BIZ_STATE_CONFLICT, "仅待审批状态的变更请求可审批");
        }
        changeRequest.setStatus(ApprovalStatus.REJECTED);
        changeRequest.setApprovedBy(SecurityUtils.getCurrentUserId());
        changeRequest.setApprovedAt(LocalDateTime.now());
        changeRequest.setApprovalComment(request == null ? null : request.getComment());
        changeRequestMapper.updateById(changeRequest);
        log.info("变更请求 {} 已驳回，意见：{}", id, request == null ? null : request.getComment());
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
