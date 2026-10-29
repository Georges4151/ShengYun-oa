package com.ceams.module.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.common.response.PageResult;
import com.ceams.module.payment.converter.CostAllocationConverter;
import com.ceams.module.payment.dto.CostAllocationCreateRequest;
import com.ceams.module.payment.dto.CostAllocationUpdateRequest;
import com.ceams.module.payment.entity.Account;
import com.ceams.module.payment.entity.CostAllocation;
import com.ceams.module.payment.enums.AllocationStatus;
import com.ceams.module.payment.mapper.AccountMapper;
import com.ceams.module.payment.mapper.CostAllocationMapper;
import com.ceams.module.payment.service.CostAllocationService;
import com.ceams.module.payment.util.PageUtil;
import com.ceams.module.payment.vo.CostAllocationVO;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    @Override
    public PageResult<CostAllocationVO> page(long page, long size, Long accountRecordId) {
        Page<CostAllocation> p = PageUtil.resolve(page, size);
        LambdaQueryWrapper<CostAllocation> wrapper = new LambdaQueryWrapper<>();
        if (accountRecordId != null) {
            wrapper.eq(CostAllocation::getAccountRecordId, accountRecordId);
        }
        wrapper.orderByDesc(CostAllocation::getCreateTime);
        IPage<CostAllocation> result = costAllocationMapper.selectPage(p, wrapper);
        return new PageResult<>(
                costAllocationConverter.toVOList(result.getRecords()),
                result.getTotal(),
                result.getCurrent(),
                result.getSize());
    }

    @Override
    public CostAllocationVO detail(Long id) {
        return costAllocationConverter.toVO(requireAllocation(id));
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
