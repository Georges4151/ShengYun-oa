package com.ceams.module.seal.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.common.response.PageResult;
import com.ceams.common.security.SecurityUtils;
import com.ceams.module.seal.converter.ElectronicSealApplicationConverter;
import com.ceams.module.seal.dto.ElectronicSealApplicationCreateRequest;
import com.ceams.module.seal.dto.ElectronicSealApplicationUpdateRequest;
import com.ceams.module.seal.entity.ElectronicSeal;
import com.ceams.module.seal.entity.ElectronicSealApplication;
import com.ceams.module.seal.entity.StampApproval;
import com.ceams.module.seal.enums.ApplicationStatus;
import com.ceams.module.seal.enums.SealStatus;
import com.ceams.module.seal.mapper.ElectronicSealApplicationMapper;
import com.ceams.module.seal.mapper.ElectronicSealMapper;
import com.ceams.module.seal.mapper.StampApprovalMapper;
import com.ceams.module.seal.service.ElectronicSealApplicationService;
import com.ceams.module.seal.support.SealAuthorizationGuard;
import com.ceams.module.seal.util.PageUtil;
import com.ceams.module.seal.vo.ElectronicSealApplicationVO;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 用印申请服务实现。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ElectronicSealApplicationServiceImpl implements ElectronicSealApplicationService {

    private final ElectronicSealApplicationMapper electronicSealApplicationMapper;
    private final ElectronicSealMapper electronicSealMapper;
    private final StampApprovalMapper stampApprovalMapper;
    private final ElectronicSealApplicationConverter electronicSealApplicationConverter;
    private final SealAuthorizationGuard sealAuthorizationGuard;

    @Override
    public PageResult<ElectronicSealApplicationVO> page(
            long page, long size, Long sealId, ApplicationStatus status, Long applicantId) {
        Page<ElectronicSealApplication> p = PageUtil.resolve(page, size);
        LambdaQueryWrapper<ElectronicSealApplication> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(sealId != null, ElectronicSealApplication::getSealId, sealId);
        wrapper.eq(status != null, ElectronicSealApplication::getStatus, status);
        wrapper.eq(applicantId != null, ElectronicSealApplication::getApplicantId, applicantId);
        wrapper.orderByDesc(ElectronicSealApplication::getCreateTime);
        IPage<ElectronicSealApplication> result =
                electronicSealApplicationMapper.selectPage(p, wrapper);
        return new PageResult<>(
                electronicSealApplicationConverter.toVOList(result.getRecords()),
                result.getTotal(),
                result.getCurrent(),
                result.getSize());
    }

    @Override
    public ElectronicSealApplicationVO detail(Long id) {
        return electronicSealApplicationConverter.toVO(requireApplication(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String create(ElectronicSealApplicationCreateRequest request) {
        ElectronicSeal seal = requireActiveSeal(request.getSealId());
        // 管理员/财务或命中有效期内授权才可提交用印申请
        sealAuthorizationGuard.checkCanUse(seal.getId());
        ElectronicSealApplication application = new ElectronicSealApplication();
        application.setSealId(seal.getId());
        application.setSealName(seal.getSealName());
        application.setApplicantId(SecurityUtils.getCurrentUserId());
        application.setApplicantName(request.getApplicantName());
        application.setPurpose(request.getPurpose());
        application.setStatus(ApplicationStatus.PENDING);
        application.setProjectId(request.getProjectId());
        application.setProjectName(request.getProjectName());
        application.setContractName(request.getContractName());
        application.setContractPath(request.getContractPath());
        application.setStampPosition(request.getStampPosition());
        application.setApplyTime(LocalDateTime.now());
        electronicSealApplicationMapper.insert(application);
        return String.valueOf(application.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, ElectronicSealApplicationUpdateRequest request) {
        ElectronicSealApplication application = requireApplication(id);
        if (application.getStatus() != ApplicationStatus.PENDING) {
            throw BusinessException.of(ErrorCode.BIZ_STATE_CONFLICT, "仅待审批状态可编辑");
        }
        if (request.getPurpose() != null) {
            application.setPurpose(request.getPurpose());
        }
        if (request.getProjectId() != null) {
            application.setProjectId(request.getProjectId());
        }
        if (request.getProjectName() != null) {
            application.setProjectName(request.getProjectName());
        }
        if (request.getContractName() != null) {
            application.setContractName(request.getContractName());
        }
        if (request.getContractPath() != null) {
            application.setContractPath(request.getContractPath());
        }
        if (request.getStampPosition() != null) {
            application.setStampPosition(request.getStampPosition());
        }
        electronicSealApplicationMapper.updateById(application);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long id) {
        ElectronicSealApplication application = requireApplication(id);
        if (application.getStatus() != ApplicationStatus.PENDING) {
            throw BusinessException.of(ErrorCode.BIZ_STATE_CONFLICT, "仅待审批状态可取消");
        }
        application.setStatus(ApplicationStatus.CANCELLED);
        electronicSealApplicationMapper.updateById(application);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        requireApplication(id);
        Long approvalCount =
                stampApprovalMapper.selectCount(
                        new LambdaQueryWrapper<StampApproval>().eq(StampApproval::getStampId, id));
        if (approvalCount != null && approvalCount > 0) {
            throw BusinessException.of(ErrorCode.BIZ_REFERENCED, "该申请存在审批记录，无法删除");
        }
        electronicSealApplicationMapper.deleteById(id);
    }

    private ElectronicSealApplication requireApplication(Long id) {
        ElectronicSealApplication application = electronicSealApplicationMapper.selectById(id);
        if (application == null) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "用印申请不存在");
        }
        return application;
    }

    private ElectronicSeal requireActiveSeal(Long sealId) {
        ElectronicSeal seal = electronicSealMapper.selectById(sealId);
        if (seal == null) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "电子印章不存在");
        }
        if (seal.getStatus() != SealStatus.ACTIVE) {
            throw BusinessException.of(ErrorCode.BIZ_STATE_CONFLICT, "印章已停用或归档，不可申请用印");
        }
        return seal;
    }
}
