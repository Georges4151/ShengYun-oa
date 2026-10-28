package com.ceams.module.seal.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.common.response.PageResult;
import com.ceams.common.security.SecurityUtils;
import com.ceams.module.seal.converter.StampApprovalConverter;
import com.ceams.module.seal.dto.StampApprovalApproveRequest;
import com.ceams.module.seal.dto.StampApprovalCreateRequest;
import com.ceams.module.seal.dto.StampApprovalRejectRequest;
import com.ceams.module.seal.dto.StampApprovalUpdateRequest;
import com.ceams.module.seal.entity.ElectronicSeal;
import com.ceams.module.seal.entity.ElectronicSealApplication;
import com.ceams.module.seal.entity.StampApproval;
import com.ceams.module.seal.enums.ApplicationStatus;
import com.ceams.module.seal.enums.ApprovalStatus;
import com.ceams.module.seal.enums.SealStatus;
import com.ceams.module.seal.mapper.ElectronicSealApplicationMapper;
import com.ceams.module.seal.mapper.ElectronicSealMapper;
import com.ceams.module.seal.mapper.StampApprovalMapper;
import com.ceams.module.seal.service.StampApprovalService;
import com.ceams.module.seal.util.PageUtil;
import com.ceams.module.seal.vo.StampApprovalVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 用印审批服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StampApprovalServiceImpl implements StampApprovalService {

    private final StampApprovalMapper stampApprovalMapper;
    private final ElectronicSealApplicationMapper electronicSealApplicationMapper;
    private final ElectronicSealMapper electronicSealMapper;
    private final StampApprovalConverter stampApprovalConverter;

    @Override
    public PageResult<StampApprovalVO> page(long page, long size, Long stampId, Long sealId, ApprovalStatus status) {
        Page<StampApproval> p = PageUtil.resolve(page, size);
        LambdaQueryWrapper<StampApproval> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(stampId != null, StampApproval::getStampId, stampId);
        wrapper.eq(sealId != null, StampApproval::getSealId, sealId);
        wrapper.eq(status != null, StampApproval::getStatus, status);
        wrapper.orderByDesc(StampApproval::getCreateTime);
        IPage<StampApproval> result = stampApprovalMapper.selectPage(p, wrapper);
        return new PageResult<>(stampApprovalConverter.toVOList(result.getRecords()),
                result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public StampApprovalVO detail(Long id) {
        return stampApprovalConverter.toVO(requireApproval(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String create(StampApprovalCreateRequest request) {
        ElectronicSealApplication application = requirePendingApplication(request.getStampId());
        ElectronicSeal seal = requireActiveSeal(application.getSealId());

        StampApproval approval = new StampApproval();
        approval.setStampId(application.getId());
        approval.setSealId(seal.getId());
        approval.setSealName(seal.getSealName());
        approval.setSealCode(seal.getSealCode());
        approval.setSealPermission(request.getSealPermission());
        approval.setProjectId(application.getProjectId());
        approval.setProjectName(application.getProjectName());
        approval.setContractName(application.getContractName());
        approval.setContractPath(application.getContractPath());
        approval.setOriginalContractPath(request.getOriginalContractPath());
        approval.setStampPositions(request.getStampPositions());
        approval.setApplicantId(application.getApplicantId());
        approval.setApplicantName(application.getApplicantName());
        approval.setStatus(ApprovalStatus.PENDING);
        approval.setApplyTime(LocalDateTime.now());
        stampApprovalMapper.insert(approval);
        return String.valueOf(approval.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, StampApprovalUpdateRequest request) {
        StampApproval approval = requireApproval(id);
        if (approval.getStatus() != ApprovalStatus.PENDING) {
            throw BusinessException.of(ErrorCode.BIZ_STATE_CONFLICT, "仅待审批状态可编辑");
        }
        if (request.getSealPermission() != null) {
            approval.setSealPermission(request.getSealPermission());
        }
        if (request.getStampPositions() != null) {
            approval.setStampPositions(request.getStampPositions());
        }
        if (request.getOriginalContractPath() != null) {
            approval.setOriginalContractPath(request.getOriginalContractPath());
        }
        stampApprovalMapper.updateById(approval);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long id, StampApprovalApproveRequest request) {
        StampApproval approval = requireApproval(id);
        if (approval.getStatus() != ApprovalStatus.PENDING) {
            throw BusinessException.of(ErrorCode.BIZ_STATE_CONFLICT, "当前状态不允许审批");
        }
        if (approval.getSealId() != null) {
            requireActiveSeal(approval.getSealId());
        }

        approval.setStatus(ApprovalStatus.APPROVED);
        approval.setApproverId(SecurityUtils.getCurrentUserId());
        approval.setApprovalTime(LocalDateTime.now());
        approval.setRejectReason(null);
        stampApprovalMapper.updateById(approval);

        updateApplicationStatus(approval.getStampId(), ApplicationStatus.APPROVED,
                approval.getApproverId(), approval.getApprovalTime(), request.getApprovalComment());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(Long id, StampApprovalRejectRequest request) {
        StampApproval approval = requireApproval(id);
        if (approval.getStatus() != ApprovalStatus.PENDING) {
            throw BusinessException.of(ErrorCode.BIZ_STATE_CONFLICT, "当前状态不允许审批");
        }

        approval.setStatus(ApprovalStatus.REJECTED);
        approval.setRejectReason(request.getRejectReason());
        approval.setApproverId(SecurityUtils.getCurrentUserId());
        approval.setApprovalTime(LocalDateTime.now());
        stampApprovalMapper.updateById(approval);

        updateApplicationStatus(approval.getStampId(), ApplicationStatus.REJECTED,
                approval.getApproverId(), approval.getApprovalTime(), request.getRejectReason());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        requireApproval(id);
        stampApprovalMapper.deleteById(id);
    }

    private StampApproval requireApproval(Long id) {
        StampApproval approval = stampApprovalMapper.selectById(id);
        if (approval == null) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "用印审批记录不存在");
        }
        return approval;
    }

    private ElectronicSealApplication requirePendingApplication(Long applicationId) {
        ElectronicSealApplication application = electronicSealApplicationMapper.selectById(applicationId);
        if (application == null) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "用印申请不存在");
        }
        if (application.getStatus() != ApplicationStatus.PENDING) {
            throw BusinessException.of(ErrorCode.BIZ_STATE_CONFLICT, "用印申请已处理，不可再发起审批");
        }
        return application;
    }

    private ElectronicSeal requireActiveSeal(Long sealId) {
        ElectronicSeal seal = electronicSealMapper.selectById(sealId);
        if (seal == null) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "电子印章不存在");
        }
        if (seal.getStatus() != SealStatus.ACTIVE) {
            throw BusinessException.of(ErrorCode.BIZ_STATE_CONFLICT, "印章已停用或归档，不可审批用印");
        }
        return seal;
    }

    private void updateApplicationStatus(Long applicationId, ApplicationStatus status,
                                         Long approverId, LocalDateTime approvalTime, String comment) {
        ElectronicSealApplication application = electronicSealApplicationMapper.selectById(applicationId);
        if (application == null) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "用印申请不存在");
        }
        application.setStatus(status);
        application.setApproverId(approverId);
        application.setApprovalComment(comment);
        application.setApprovalTime(approvalTime);
        electronicSealApplicationMapper.updateById(application);
    }
}
