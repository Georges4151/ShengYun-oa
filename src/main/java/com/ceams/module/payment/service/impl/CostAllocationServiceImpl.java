package com.ceams.module.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.common.response.PageResult;
import com.ceams.module.iam.entity.SysDept;
import com.ceams.module.iam.mapper.DeptMapper;
import com.ceams.module.payment.converter.CostAllocationConverter;
import com.ceams.module.payment.dto.CostAllocationCreateRequest;
import com.ceams.module.payment.dto.CostAllocationUpdateRequest;
import com.ceams.module.payment.entity.Account;
import com.ceams.module.payment.entity.CostAllocation;
import com.ceams.module.payment.enums.AllocationStatus;
import com.ceams.module.payment.enums.TargetType;
import com.ceams.module.payment.mapper.AccountMapper;
import com.ceams.module.payment.mapper.CostAllocationMapper;
import com.ceams.module.payment.service.CostAllocationService;
import com.ceams.module.payment.util.PageUtil;
import com.ceams.module.payment.vo.CostAllocationVO;
import com.ceams.module.project.entity.Project;
import com.ceams.module.project.entity.SubcontractorContract;
import com.ceams.module.project.mapper.ProjectMapper;
import com.ceams.module.project.mapper.SubcontractorContractMapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 成本分摊服务实现。
 *
 * <p>分摊约束：同一账户记录下所有分摊金额合计（含本次）不得超出记录金额， 每次变更后回写 biz_account.allocation_status。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CostAllocationServiceImpl implements CostAllocationService {

    private final CostAllocationMapper costAllocationMapper;
    private final AccountMapper accountMapper;
    private final CostAllocationConverter costAllocationConverter;
    private final ProjectMapper projectMapper;
    private final SubcontractorContractMapper subcontractorContractMapper;
    private final DeptMapper deptMapper;

    @Override
    public PageResult<CostAllocationVO> page(long page, long size, Long accountRecordId) {
        Page<CostAllocation> p = PageUtil.resolve(page, size);
        LambdaQueryWrapper<CostAllocation> wrapper = new LambdaQueryWrapper<>();
        if (accountRecordId != null) {
            wrapper.eq(CostAllocation::getAccountRecordId, accountRecordId);
        }
        wrapper.orderByDesc(CostAllocation::getCreateTime);
        IPage<CostAllocation> result = costAllocationMapper.selectPage(p, wrapper);
        List<CostAllocationVO> records = costAllocationConverter.toVOList(result.getRecords());
        fillNames(records);
        return new PageResult<>(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public CostAllocationVO detail(Long id) {
        CostAllocationVO vo = costAllocationConverter.toVO(requireAllocation(id));
        fillNames(List.of(vo));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String create(CostAllocationCreateRequest request) {
        Account account = requireAccountRecord(request.getAccountRecordId());
        BigDecimal allocated = sumAllocated(request.getAccountRecordId(), null);
        BigDecimal requestAmount =
                request.getAllocationAmount() == null
                        ? BigDecimal.ZERO
                        : request.getAllocationAmount();
        BigDecimal total = account.getAmount() == null ? BigDecimal.ZERO : account.getAmount();
        if (allocated.add(requestAmount).compareTo(total) > 0) {
            throw BusinessException.of(ErrorCode.BIZ_UNPROCESSABLE, "分摊金额合计不能超过记录金额");
        }

        CostAllocation entity = new CostAllocation();
        entity.setAccountRecordId(request.getAccountRecordId());
        entity.setTargetType(request.getTargetType());
        entity.setTargetId(request.getTargetId());
        entity.setAllocationAmount(request.getAllocationAmount());
        entity.setAllocationRatio(request.getAllocationRatio());
        costAllocationMapper.insert(entity);

        refreshAllocationStatus(request.getAccountRecordId());
        return String.valueOf(entity.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, CostAllocationUpdateRequest request) {
        CostAllocation entity = requireAllocation(id);
        Account account = requireAccountRecord(entity.getAccountRecordId());

        BigDecimal newAmount = entity.getAllocationAmount();
        if (request.getAllocationAmount() != null) {
            newAmount = request.getAllocationAmount();
        }
        BigDecimal others = sumAllocated(entity.getAccountRecordId(), id);
        BigDecimal total = account.getAmount() == null ? BigDecimal.ZERO : account.getAmount();
        if (others.add(newAmount).compareTo(total) > 0) {
            throw BusinessException.of(ErrorCode.BIZ_UNPROCESSABLE, "分摊金额合计不能超过记录金额");
        }

        if (request.getTargetType() != null) {
            entity.setTargetType(request.getTargetType());
        }
        if (request.getTargetId() != null) {
            entity.setTargetId(request.getTargetId());
        }
        if (request.getAllocationAmount() != null) {
            entity.setAllocationAmount(request.getAllocationAmount());
        }
        if (request.getAllocationRatio() != null) {
            entity.setAllocationRatio(request.getAllocationRatio());
        }
        costAllocationMapper.updateById(entity);

        refreshAllocationStatus(entity.getAccountRecordId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        CostAllocation entity = requireAllocation(id);
        Long accountRecordId = entity.getAccountRecordId();
        costAllocationMapper.deleteById(id);
        refreshAllocationStatus(accountRecordId);
    }

    /**
     * 批量回填展示名称：流水记录名称 + 分摊目标名称，避免逐条查询造成 N+1。
     *
     * <p>accountRecordName：优先流水所属项目名称，其次流水描述，均无则为 null；
     * targetName：按 targetType 分别批量查询项目 / 合同 / 部门，SERVICE 无主数据来源保持 null。
     */
    private void fillNames(List<CostAllocationVO> records) {
        if (records == null || records.isEmpty()) {
            return;
        }

        List<Long> accountRecordIds =
                records.stream()
                        .map(CostAllocationVO::getAccountRecordId)
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList();
        Map<Long, Account> accounts = new HashMap<>();
        if (!accountRecordIds.isEmpty()) {
            for (Account account : accountMapper.selectBatchIds(accountRecordIds)) {
                accounts.put(account.getId(), account);
            }
        }

        // 流水所属项目与 PROJECT 类型分摊目标合并为一次批量查询
        List<Long> projectIds = new ArrayList<>();
        for (Account account : accounts.values()) {
            if (account.getProjectId() != null) {
                projectIds.add(account.getProjectId());
            }
        }
        projectIds.addAll(distinctTargetIds(records, TargetType.PROJECT));
        List<Long> mergedProjectIds = projectIds.stream().distinct().toList();
        Map<Long, String> projectNames = new HashMap<>();
        if (!mergedProjectIds.isEmpty()) {
            for (Project project : projectMapper.selectBatchIds(mergedProjectIds)) {
                projectNames.put(project.getId(), project.getProjectName());
            }
        }

        List<Long> contractIds = distinctTargetIds(records, TargetType.CONTRACT);
        Map<Long, String> contractNames = new HashMap<>();
        if (!contractIds.isEmpty()) {
            for (SubcontractorContract contract :
                    subcontractorContractMapper.selectBatchIds(contractIds)) {
                contractNames.put(contract.getId(), contract.getContractName());
            }
        }

        List<Long> deptIds = distinctTargetIds(records, TargetType.DEPT);
        Map<Long, String> deptNames = new HashMap<>();
        if (!deptIds.isEmpty()) {
            for (SysDept dept : deptMapper.selectBatchIds(deptIds)) {
                deptNames.put(dept.getId(), dept.getName());
            }
        }

        for (CostAllocationVO record : records) {
            record.setAccountRecordName(
                    resolveAccountRecordName(record.getAccountRecordId(), accounts, projectNames));
            record.setTargetName(resolveTargetName(record, projectNames, contractNames, deptNames));
        }
    }

    /** 流水记录展示名称：优先所属项目名称，其次流水描述。 */
    private String resolveAccountRecordName(
            Long accountRecordId, Map<Long, Account> accounts, Map<Long, String> projectNames) {
        Account account = accountRecordId == null ? null : accounts.get(accountRecordId);
        if (account == null) {
            return null;
        }
        if (account.getProjectId() != null) {
            String projectName = projectNames.get(account.getProjectId());
            if (StringUtils.isNotBlank(projectName)) {
                return projectName;
            }
        }
        return StringUtils.isNotBlank(account.getDescription()) ? account.getDescription() : null;
    }

    /** 分摊目标名称：按目标类型取对应主数据名称，SERVICE 无来源返回 null。 */
    private String resolveTargetName(
            CostAllocationVO record,
            Map<Long, String> projectNames,
            Map<Long, String> contractNames,
            Map<Long, String> deptNames) {
        TargetType targetType = record.getTargetType();
        if (targetType == null || record.getTargetId() == null) {
            return null;
        }
        switch (targetType) {
            case PROJECT:
                return projectNames.get(record.getTargetId());
            case CONTRACT:
                return contractNames.get(record.getTargetId());
            case DEPT:
                return deptNames.get(record.getTargetId());
            default:
                return null;
        }
    }

    /** 按目标类型收集去重后的非空目标 ID。 */
    private static List<Long> distinctTargetIds(
            List<CostAllocationVO> records, TargetType targetType) {
        return records.stream()
                .filter(record -> record.getTargetType() == targetType)
                .map(CostAllocationVO::getTargetId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private CostAllocation requireAllocation(Long id) {
        CostAllocation entity = costAllocationMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "成本分摊不存在");
        }
        return entity;
    }

    private Account requireAccountRecord(Long id) {
        Account account = accountMapper.selectById(id);
        if (account == null) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "收支流水不存在");
        }
        return account;
    }

    /** 汇总某账户记录下（排除 excludeId）所有分摊金额。 */
    private BigDecimal sumAllocated(Long accountRecordId, Long excludeId) {
        LambdaQueryWrapper<CostAllocation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CostAllocation::getAccountRecordId, accountRecordId);
        if (excludeId != null) {
            wrapper.ne(CostAllocation::getId, excludeId);
        }
        List<CostAllocation> list = costAllocationMapper.selectList(wrapper);
        return list.stream()
                .map(CostAllocation::getAllocationAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** 回写 biz_account.allocation_status。 */
    private void refreshAllocationStatus(Long accountRecordId) {
        Account account = requireAccountRecord(accountRecordId);
        BigDecimal allocated = sumAllocated(accountRecordId, null);
        BigDecimal total = account.getAmount() == null ? BigDecimal.ZERO : account.getAmount();
        AllocationStatus status;
        if (allocated.compareTo(total) >= 0) {
            status = AllocationStatus.ALLOCATED;
        } else if (allocated.compareTo(BigDecimal.ZERO) > 0) {
            status = AllocationStatus.PARTIAL;
        } else {
            status = AllocationStatus.UNALLOCATED;
        }
        account.setAllocationStatus(status);
        accountMapper.updateById(account);
    }
}
