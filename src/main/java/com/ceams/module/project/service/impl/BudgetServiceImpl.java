package com.ceams.module.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.common.response.PageResult;
import com.ceams.module.project.converter.BudgetConverter;
import com.ceams.module.project.dto.budget.BudgetCreateRequest;
import com.ceams.module.project.dto.budget.BudgetUpdateRequest;
import com.ceams.module.project.entity.Budget;
import com.ceams.module.project.entity.Project;
import com.ceams.module.project.entity.SubcontractorContract;
import com.ceams.module.project.mapper.BudgetMapper;
import com.ceams.module.project.mapper.ProjectMapper;
import com.ceams.module.project.mapper.SubcontractorContractMapper;
import com.ceams.module.project.service.BudgetService;
import com.ceams.module.project.util.PageUtil;
import com.ceams.module.project.vo.BudgetVO;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 预算服务实现。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BudgetServiceImpl implements BudgetService {

    private final BudgetMapper budgetMapper;
    private final ProjectMapper projectMapper;
    private final SubcontractorContractMapper subcontractorContractMapper;
    private final BudgetConverter budgetConverter;

    @Override
    public PageResult<BudgetVO> page(long page, long size, Long projectId) {
        Page<Budget> p = PageUtil.resolve(page, size);
        LambdaQueryWrapper<Budget> wrapper = new LambdaQueryWrapper<>();
        if (projectId != null) {
            wrapper.eq(Budget::getProjectId, projectId);
        }
        wrapper.orderByDesc(Budget::getCreateTime);
        IPage<Budget> result = budgetMapper.selectPage(p, wrapper);
        return new PageResult<>(
                budgetConverter.toVOList(result.getRecords()),
                result.getTotal(),
                result.getCurrent(),
                result.getSize());
    }

    @Override
    public BudgetVO detail(Long id) {
        return budgetConverter.toVO(requireBudget(id));
    }

    @Override
    public PageResult<BudgetVO> pageForManager(
            long page, long size, Long projectId, Long managerId) {
        if (managerId == null) {
            return new PageResult<>(List.of(), 0L, page, size);
        }
        List<Project> projects =
                projectMapper.selectList(
                        new LambdaQueryWrapper<Project>().eq(Project::getManagerId, managerId));
        if (projects.isEmpty()) {
            return new PageResult<>(List.of(), 0L, page, size);
        }
        List<Long> projectIds = projects.stream().map(Project::getId).toList();
        if (projectId != null) {
            if (!projectIds.contains(projectId)) {
                return new PageResult<>(List.of(), 0L, page, size);
            }
            projectIds = List.of(projectId);
        }
        Page<Budget> p = PageUtil.resolve(page, size);
        LambdaQueryWrapper<Budget> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Budget::getProjectId, projectIds).orderByDesc(Budget::getCreateTime);
        IPage<Budget> result = budgetMapper.selectPage(p, wrapper);
        return new PageResult<>(
                budgetConverter.toVOList(result.getRecords()),
                result.getTotal(),
                result.getCurrent(),
                result.getSize());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String create(BudgetCreateRequest request) {
        if (request.getProjectId() != null) {
            requireProject(request.getProjectId());
        }
        if (request.getContractId() != null) {
            requireContract(request.getContractId());
        }
        Budget budget = new Budget();
        budget.setBudgetName(request.getBudgetName());
        budget.setAmount(request.getAmount());
        budget.setProjectId(request.getProjectId());
        budget.setContractId(request.getContractId());
        budget.setStartDate(toDateTime(request.getStartDate()));
        budget.setEndDate(toDateTime(request.getEndDate()));
        budget.setStatus(request.getStatus());
        budget.setUsedAmount(BigDecimal.ZERO);
        budget.setRemainingAmount(request.getAmount());
        budgetMapper.insert(budget);
        return String.valueOf(budget.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, BudgetUpdateRequest request) {
        Budget budget = requireBudget(id);
        if (StringUtils.isNotBlank(request.getBudgetName())) {
            budget.setBudgetName(request.getBudgetName());
        }
        if (request.getAmount() != null) {
            budget.setAmount(request.getAmount());
            budget.setRemainingAmount(
                    recomputeRemaining(request.getAmount(), budget.getUsedAmount()));
        }
        if (request.getProjectId() != null) {
            requireProject(request.getProjectId());
            budget.setProjectId(request.getProjectId());
        }
        if (request.getContractId() != null) {
            requireContract(request.getContractId());
            budget.setContractId(request.getContractId());
        }
        if (request.getStartDate() != null) {
            budget.setStartDate(toDateTime(request.getStartDate()));
        }
        if (request.getEndDate() != null) {
            budget.setEndDate(toDateTime(request.getEndDate()));
        }
        if (request.getStatus() != null) {
            budget.setStatus(request.getStatus());
        }
        budgetMapper.updateById(budget);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        requireBudget(id);
        budgetMapper.deleteById(id);
    }

    private Budget requireBudget(Long id) {
        Budget budget = budgetMapper.selectById(id);
        if (budget == null) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "预算不存在");
        }
        return budget;
    }

    private void requireProject(Long projectId) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "项目不存在");
        }
    }

    private void requireContract(Long contractId) {
        SubcontractorContract contract = subcontractorContractMapper.selectById(contractId);
        if (contract == null) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "分包合同不存在");
        }
    }

    private BigDecimal recomputeRemaining(BigDecimal amount, BigDecimal usedAmount) {
        BigDecimal used = usedAmount == null ? BigDecimal.ZERO : usedAmount;
        return amount.subtract(used);
    }

    private LocalDateTime toDateTime(LocalDate date) {
        return date == null ? null : date.atStartOfDay();
    }
}
