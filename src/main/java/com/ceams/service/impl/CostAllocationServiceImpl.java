package com.ceams.service.impl;

import com.ceams.dao.CostAllocationMapper;
import com.ceams.model.entity.CostAllocation;
import com.ceams.model.Pagination;
import com.ceams.service.CostAllocationService;
import com.ceams.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CostAllocationServiceImpl implements CostAllocationService {

    @Autowired
    private CostAllocationMapper costAllocationMapper;

    @Autowired
    private AccountService accountService;

    @Override
    public List<CostAllocation> getAllAllocations() {
        return costAllocationMapper.selectAll();
    }

    @Override
    public List<CostAllocation> getByAccountRecordId(Integer accountRecordId) {
        return costAllocationMapper.selectByAccountRecordId(accountRecordId);
    }

    @Override
    public List<CostAllocation> getByTargetIdAndType(Integer targetId, String targetType) {
        return costAllocationMapper.selectByTargetIdAndType(targetId, targetType);
    }

    @Override
    public CostAllocation getAllocationById(Integer id) {
        return costAllocationMapper.selectById(id);
    }

    @Override
    public boolean addAllocation(CostAllocation costAllocation) {
        boolean result = costAllocationMapper.insert(costAllocation) > 0;
        if (result) {
            // 更新账目记录的成本分摊状态
            updateAccountAllocationStatus(costAllocation.getAccountRecordId());
        }
        return result;
    }

    @Override
    public boolean updateAllocation(CostAllocation costAllocation) {
        boolean result = costAllocationMapper.update(costAllocation) > 0;
        if (result) {
            // 更新账目记录的成本分摊状态
            updateAccountAllocationStatus(costAllocation.getAccountRecordId());
        }
        return result;
    }

    @Override
    public boolean deleteAllocation(Integer id) {
        CostAllocation allocation = costAllocationMapper.selectById(id);
        Integer accountRecordId = allocation.getAccountRecordId();
        boolean result = costAllocationMapper.deleteById(id) > 0;
        if (result) {
            // 更新账目记录的成本分摊状态
            updateAccountAllocationStatus(accountRecordId);
        }
        return result;
    }

    @Override
    public List<CostAllocation> getAllocationsWithPagination(Pagination pagination) {
        return costAllocationMapper.selectAllWithPagination(pagination);
    }

    @Override
    public List<CostAllocation> getAllocationsByConditionsWithPagination(Integer accountRecordId, String targetType, Pagination pagination) {
        return costAllocationMapper.selectByConditionsWithPagination(accountRecordId, targetType, pagination);
    }

    @Override
    public long count() {
        return costAllocationMapper.count();
    }

    @Override
    public int countByConditions(Integer accountRecordId, String targetType) {
        return costAllocationMapper.countByConditions(accountRecordId, targetType);
    }

    /**
     * 更新账目记录的成本分摊状态
     * @param accountRecordId 账目记录ID
     */
    private void updateAccountAllocationStatus(Integer accountRecordId) {
        // 这里可以实现更新账目记录成本分摊状态的逻辑
        // 例如，检查是否所有成本都已分摊，然后更新状态为completed
    }
}
