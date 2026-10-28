package com.ceams.module.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.common.response.PageResult;
import com.ceams.module.project.converter.SubcontractorContractConverter;
import com.ceams.module.project.dto.subcontractorcontract.SubcontractorContractCreateRequest;
import com.ceams.module.project.dto.subcontractorcontract.SubcontractorContractUpdateRequest;
import com.ceams.module.project.entity.Budget;
import com.ceams.module.project.entity.Project;
import com.ceams.module.project.entity.SettlementProof;
import com.ceams.module.project.entity.Subcontractor;
import com.ceams.module.project.entity.SubcontractorContract;
import com.ceams.module.project.mapper.BudgetMapper;
import com.ceams.module.project.mapper.ProjectMapper;
import com.ceams.module.project.mapper.SettlementProofMapper;
import com.ceams.module.project.mapper.SubcontractorContractMapper;
import com.ceams.module.project.mapper.SubcontractorMapper;
import com.ceams.module.project.service.SubcontractorContractService;
import com.ceams.module.project.util.PageUtil;
import com.ceams.module.project.vo.SubcontractorContractVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 分包合同服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubcontractorContractServiceImpl implements SubcontractorContractService {

    private final SubcontractorContractMapper subcontractorContractMapper;
    private final SubcontractorMapper subcontractorMapper;
    private final ProjectMapper projectMapper;
    private final BudgetMapper budgetMapper;
    private final SettlementProofMapper settlementProofMapper;
    private final SubcontractorContractConverter subcontractorContractConverter;

    @Override
    public PageResult<SubcontractorContractVO> page(long page, long size, Long subcontractorId, Long projectId) {
        Page<SubcontractorContract> p = PageUtil.resolve(page, size);
        LambdaQueryWrapper<SubcontractorContract> wrapper = new LambdaQueryWrapper<>();
        if (subcontractorId != null) {
            wrapper.eq(SubcontractorContract::getSubcontractorId, subcontractorId);
        }
        if (projectId != null) {
            wrapper.eq(SubcontractorContract::getProjectId, projectId);
        }
        wrapper.orderByDesc(SubcontractorContract::getCreateTime);
        IPage<SubcontractorContract> result = subcontractorContractMapper.selectPage(p, wrapper);
        return new PageResult<>(subcontractorContractConverter.toVOList(result.getRecords()),
                result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public SubcontractorContractVO detail(Long id) {
        return subcontractorContractConverter.toVO(requireContract(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String create(SubcontractorContractCreateRequest request) {
        requireSubcontractor(request.getSubcontractorId());
        if (request.getProjectId() != null) {
            requireProject(request.getProjectId());
        }
        checkNumberUnique(request.getContractNumber(), null);
        SubcontractorContract contract = new SubcontractorContract();
        contract.setSubcontractorId(request.getSubcontractorId());
        contract.setProjectId(request.getProjectId());
        contract.setContractName(request.getContractName());
        contract.setContractNumber(request.getContractNumber());
        contract.setContractAmount(request.getContractAmount());
        contract.setStartDate(toDateTime(request.getStartDate()));
        contract.setEndDate(toDateTime(request.getEndDate()));
        contract.setStatus(request.getStatus());
        contract.setDescription(request.getDescription());
        subcontractorContractMapper.insert(contract);
        return String.valueOf(contract.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, SubcontractorContractUpdateRequest request) {
        SubcontractorContract contract = requireContract(id);
        if (request.getProjectId() != null) {
            requireProject(request.getProjectId());
            contract.setProjectId(request.getProjectId());
        }
        if (StringUtils.isNotBlank(request.getContractName())) {
            contract.setContractName(request.getContractName());
        }
        if (request.getContractAmount() != null) {
            contract.setContractAmount(request.getContractAmount());
        }
        if (request.getStartDate() != null) {
            contract.setStartDate(toDateTime(request.getStartDate()));
        }
        if (request.getEndDate() != null) {
            contract.setEndDate(toDateTime(request.getEndDate()));
        }
        if (request.getStatus() != null) {
            contract.setStatus(request.getStatus());
        }
        if (request.getDescription() != null) {
            contract.setDescription(request.getDescription());
        }
        subcontractorContractMapper.updateById(contract);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        requireContract(id);
        Long budgetRefs = budgetMapper.selectCount(new LambdaQueryWrapper<Budget>()
                .eq(Budget::getContractId, id));
        if (budgetRefs != null && budgetRefs > 0) {
            throw BusinessException.of(ErrorCode.BIZ_REFERENCED, "分包合同存在关联预算，无法删除");
        }
        Long proofRefs = settlementProofMapper.selectCount(new LambdaQueryWrapper<SettlementProof>()
                .eq(SettlementProof::getContractId, id));
        if (proofRefs != null && proofRefs > 0) {
            throw BusinessException.of(ErrorCode.BIZ_REFERENCED, "分包合同存在关联结算凭证，无法删除");
        }
        subcontractorContractMapper.deleteById(id);
    }

    private SubcontractorContract requireContract(Long id) {
        SubcontractorContract contract = subcontractorContractMapper.selectById(id);
        if (contract == null) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "分包合同不存在");
        }
        return contract;
    }

    private void requireSubcontractor(Long subcontractorId) {
        Subcontractor subcontractor = subcontractorMapper.selectById(subcontractorId);
        if (subcontractor == null) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "分包商不存在");
        }
    }

    private void requireProject(Long projectId) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "项目不存在");
        }
    }

    private void checkNumberUnique(String contractNumber, Long excludeId) {
        LambdaQueryWrapper<SubcontractorContract> wrapper = new LambdaQueryWrapper<SubcontractorContract>()
                .eq(SubcontractorContract::getContractNumber, contractNumber);
        if (excludeId != null) {
            wrapper.ne(SubcontractorContract::getId, excludeId);
        }
        Long count = subcontractorContractMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw BusinessException.of(ErrorCode.BIZ_DUPLICATE_CODE, "合同编号已存在");
        }
    }

    private LocalDateTime toDateTime(LocalDate date) {
        return date == null ? null : date.atStartOfDay();
    }
}