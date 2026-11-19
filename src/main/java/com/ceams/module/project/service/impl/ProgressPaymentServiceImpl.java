package com.ceams.module.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.common.response.PageResult;
import com.ceams.common.security.SecurityUtils;
import com.ceams.module.project.converter.ProgressPaymentConverter;
import com.ceams.module.project.dto.progresspayment.ProgressPaymentCreateRequest;
import com.ceams.module.project.dto.progresspayment.ProgressPaymentUpdateRequest;
import com.ceams.module.project.entity.ProgressPayment;
import com.ceams.module.project.entity.Project;
import com.ceams.module.project.enums.ApprovalStatus;
import com.ceams.module.project.mapper.ProgressPaymentMapper;
import com.ceams.module.project.mapper.ProjectMapper;
import com.ceams.module.project.service.ProgressPaymentService;
import com.ceams.module.project.util.PageUtil;
import com.ceams.module.project.vo.ProgressPaymentVO;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 进度款服务实现。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProgressPaymentServiceImpl implements ProgressPaymentService {

    private final ProgressPaymentMapper progressPaymentMapper;
    private final ProjectMapper projectMapper;
    private final ProgressPaymentConverter progressPaymentConverter;

    @Override
    public PageResult<ProgressPaymentVO> page(
            long page, long size, Long projectId, ApprovalStatus status) {
        Page<ProgressPayment> p = PageUtil.resolve(page, size);
        LambdaQueryWrapper<ProgressPayment> wrapper = new LambdaQueryWrapper<>();
        if (projectId != null) {
            wrapper.eq(ProgressPayment::getProjectId, projectId);
        }
        if (status != null) {
            wrapper.eq(ProgressPayment::getStatus, status);
        }
        wrapper.orderByDesc(ProgressPayment::getCreateTime);
        IPage<ProgressPayment> result = progressPaymentMapper.selectPage(p, wrapper);
        List<ProgressPaymentVO> records = progressPaymentConverter.toVOList(result.getRecords());
        fillProjectNames(records);
        return new PageResult<>(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public ProgressPaymentVO detail(Long id) {
        ProgressPaymentVO vo = progressPaymentConverter.toVO(requireProgressPayment(id));
        fillProjectNames(List.of(vo));
        return vo;
    }

    @Override
    public PageResult<ProgressPaymentVO> pageForApplicant(
            long page, long size, Long projectId, ApprovalStatus status, Long applicantId) {
        if (applicantId == null) {
            return new PageResult<>(List.of(), 0L, page, size);
        }
        Page<ProgressPayment> p = PageUtil.resolve(page, size);
        LambdaQueryWrapper<ProgressPayment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProgressPayment::getApplicationBy, applicantId);
        if (projectId != null) {
            wrapper.eq(ProgressPayment::getProjectId, projectId);
        }
        if (status != null) {
            wrapper.eq(ProgressPayment::getStatus, status);
        }
        wrapper.orderByDesc(ProgressPayment::getCreateTime);
        IPage<ProgressPayment> result = progressPaymentMapper.selectPage(p, wrapper);
        List<ProgressPaymentVO> records = progressPaymentConverter.toVOList(result.getRecords());
        fillProjectNames(records);
        return new PageResult<>(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String create(ProgressPaymentCreateRequest request) {
        requireProject(request.getProjectId());
        ProgressPayment payment = new ProgressPayment();
        payment.setProjectId(request.getProjectId());
        payment.setPaymentPeriod(request.getPaymentPeriod());
        payment.setApplicationAmount(request.getApplicationAmount());
        payment.setApplicationDate(toDateTime(request.getApplicationDate()));
        payment.setStatus(ApprovalStatus.PENDING);
        payment.setApplicationBy(SecurityUtils.getCurrentUserId());
        progressPaymentMapper.insert(payment);
        return String.valueOf(payment.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, ProgressPaymentUpdateRequest request) {
        ProgressPayment payment = requireProgressPayment(id);
        if (StringUtils.isNotBlank(request.getPaymentPeriod())) {
            payment.setPaymentPeriod(request.getPaymentPeriod());
        }
        if (request.getApplicationAmount() != null) {
            payment.setApplicationAmount(request.getApplicationAmount());
        }
        if (request.getApprovedAmount() != null) {
            payment.setApprovedAmount(request.getApprovedAmount());
        }
        if (request.getPaymentAmount() != null) {
            payment.setPaymentAmount(request.getPaymentAmount());
        }
        if (request.getApplicationDate() != null) {
            payment.setApplicationDate(toDateTime(request.getApplicationDate()));
        }
        if (request.getApprovalDate() != null) {
            payment.setApprovalDate(toDateTime(request.getApprovalDate()));
        }
        if (request.getPaymentDate() != null) {
            payment.setPaymentDate(toDateTime(request.getPaymentDate()));
        }
        if (request.getStatus() != null) {
            payment.setStatus(request.getStatus());
        }
        progressPaymentMapper.updateById(payment);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        requireProgressPayment(id);
        progressPaymentMapper.deleteById(id);
    }

    /** 批量回填项目名称，避免逐条查询造成 N+1。 */
    private void fillProjectNames(List<ProgressPaymentVO> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        List<Long> projectIds =
                records.stream()
                        .map(ProgressPaymentVO::getProjectId)
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
        for (ProgressPaymentVO record : records) {
            if (record.getProjectId() != null) {
                record.setProjectName(projectNames.get(record.getProjectId()));
            }
        }
    }

    private ProgressPayment requireProgressPayment(Long id) {
        ProgressPayment payment = progressPaymentMapper.selectById(id);
        if (payment == null) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "进度款不存在");
        }
        return payment;
    }

    private void requireProject(Long projectId) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "项目不存在");
        }
    }

    private LocalDateTime toDateTime(LocalDate date) {
        return date == null ? null : date.atStartOfDay();
    }
}
