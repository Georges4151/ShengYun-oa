package com.ceams.module.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.common.response.PageResult;
import com.ceams.module.project.converter.ProjectConverter;
import com.ceams.module.project.dto.project.ProjectCreateRequest;
import com.ceams.module.project.dto.project.ProjectUpdateRequest;
import com.ceams.module.project.entity.Budget;
import com.ceams.module.project.entity.ChangeRequest;
import com.ceams.module.project.entity.ProgressPayment;
import com.ceams.module.project.entity.Project;
import com.ceams.module.project.entity.SubcontractorContract;
import com.ceams.module.project.enums.ProjectStatus;
import com.ceams.module.project.mapper.BudgetMapper;
import com.ceams.module.project.mapper.ChangeRequestMapper;
import com.ceams.module.project.mapper.ProgressPaymentMapper;
import com.ceams.module.project.mapper.ProjectMapper;
import com.ceams.module.project.mapper.SubcontractorContractMapper;
import com.ceams.module.project.service.ProjectService;
import com.ceams.module.project.util.PageUtil;
import com.ceams.module.project.vo.ProjectVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 项目台账服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {

    private final ProjectMapper projectMapper;
    private final BudgetMapper budgetMapper;
    private final ChangeRequestMapper changeRequestMapper;
    private final ProgressPaymentMapper progressPaymentMapper;
    private final SubcontractorContractMapper subcontractorContractMapper;
    private final ProjectConverter projectConverter;

    @Override
    public PageResult<ProjectVO> page(long page, long size, String keyword, ProjectStatus status) {
        Page<Project> p = PageUtil.resolve(page, size);
        LambdaQueryWrapper<Project> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(keyword)) {
            wrapper.and(w -> w.like(Project::getProjectName, keyword)
                    .or().like(Project::getClientName, keyword));
        }
        if (status != null) {
            wrapper.eq(Project::getStatus, status);
        }
        wrapper.orderByDesc(Project::getCreateTime);
        IPage<Project> result = projectMapper.selectPage(p, wrapper);
        return new PageResult<>(projectConverter.toVOList(result.getRecords()),
                result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public ProjectVO detail(Long id) {
        return projectConverter.toVO(requireProject(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String create(ProjectCreateRequest request) {
        Project project = new Project();
        project.setProjectName(request.getProjectName());
        project.setClientName(request.getClientName());
        project.setProjectAddress(request.getProjectAddress());
        project.setProjectType(request.getProjectType());
        project.setStartDate(toDateTime(request.getStartDate()));
        project.setEndDate(toDateTime(request.getEndDate()));
        project.setContractAmount(request.getContractAmount());
        project.setBudgetAmount(request.getBudgetAmount());
        project.setStatus(request.getStatus());
        project.setDescription(request.getDescription());
        project.setManagerId(request.getManagerId());
        projectMapper.insert(project);
        return String.valueOf(project.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, ProjectUpdateRequest request) {
        Project project = requireProject(id);
        if (StringUtils.isNotBlank(request.getProjectName())) {
            project.setProjectName(request.getProjectName());
        }
        if (StringUtils.isNotBlank(request.getClientName())) {
            project.setClientName(request.getClientName());
        }
        if (request.getProjectAddress() != null) {
            project.setProjectAddress(request.getProjectAddress());
        }
        if (request.getProjectType() != null) {
            project.setProjectType(request.getProjectType());
        }
        if (request.getStartDate() != null) {
            project.setStartDate(toDateTime(request.getStartDate()));
        }
        if (request.getEndDate() != null) {
            project.setEndDate(toDateTime(request.getEndDate()));
        }
        if (request.getContractAmount() != null) {
            project.setContractAmount(request.getContractAmount());
        }
        if (request.getBudgetAmount() != null) {
            project.setBudgetAmount(request.getBudgetAmount());
        }
        if (request.getStatus() != null) {
            project.setStatus(request.getStatus());
        }
        if (request.getDescription() != null) {
            project.setDescription(request.getDescription());
        }
        if (request.getManagerId() != null) {
            project.setManagerId(request.getManagerId());
        }
        projectMapper.updateById(project);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        requireProject(id);
        Long budgetRefs = budgetMapper.selectCount(new LambdaQueryWrapper<Budget>()
                .eq(Budget::getProjectId, id));
        if (budgetRefs != null && budgetRefs > 0) {
            throw BusinessException.of(ErrorCode.BIZ_REFERENCED, "项目存在关联预算，无法删除");
        }
        Long changeRefs = changeRequestMapper.selectCount(new LambdaQueryWrapper<ChangeRequest>()
                .eq(ChangeRequest::getProjectId, id));
        if (changeRefs != null && changeRefs > 0) {
            throw BusinessException.of(ErrorCode.BIZ_REFERENCED, "项目存在关联变更请求，无法删除");
        }
        Long paymentRefs = progressPaymentMapper.selectCount(new LambdaQueryWrapper<ProgressPayment>()
                .eq(ProgressPayment::getProjectId, id));
        if (paymentRefs != null && paymentRefs > 0) {
            throw BusinessException.of(ErrorCode.BIZ_REFERENCED, "项目存在关联进度款，无法删除");
        }
        Long contractRefs = subcontractorContractMapper.selectCount(new LambdaQueryWrapper<SubcontractorContract>()
                .eq(SubcontractorContract::getProjectId, id));
        if (contractRefs != null && contractRefs > 0) {
            throw BusinessException.of(ErrorCode.BIZ_REFERENCED, "项目存在关联分包合同，无法删除");
        }
        projectMapper.deleteById(id);
    }

    private Project requireProject(Long id) {
        Project project = projectMapper.selectById(id);
        if (project == null) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "项目不存在");
        }
        return project;
    }

    private LocalDateTime toDateTime(LocalDate date) {
        return date == null ? null : date.atStartOfDay();
    }
}