package com.ceams.module.hr.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.common.response.PageResult;
import com.ceams.module.hr.converter.HrContractConverter;
import com.ceams.module.hr.dto.ContractCreateRequest;
import com.ceams.module.hr.dto.ContractUpdateRequest;
import com.ceams.module.hr.entity.HrContract;
import com.ceams.module.hr.entity.HrEmployee;
import com.ceams.module.hr.enums.ContractStatus;
import com.ceams.module.hr.mapper.HrContractMapper;
import com.ceams.module.hr.mapper.HrEmployeeMapper;
import com.ceams.module.hr.service.HrContractService;
import com.ceams.module.hr.util.HrPageUtil;
import com.ceams.module.hr.vo.ContractVO;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 劳动合同服务实现。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HrContractServiceImpl implements HrContractService {

    private final HrContractMapper contractMapper;
    private final HrEmployeeMapper employeeMapper;
    private final HrContractConverter contractConverter;

    @Override
    public PageResult<ContractVO> page(
            long page, long size, Long employeeId, ContractStatus status) {
        Page<HrContract> p = HrPageUtil.resolve(page, size);
        LambdaQueryWrapper<HrContract> wrapper = new LambdaQueryWrapper<>();
        if (employeeId != null) {
            wrapper.eq(HrContract::getEmployeeId, employeeId);
        }
        if (status != null) {
            wrapper.eq(HrContract::getStatus, status);
        }
        wrapper.orderByDesc(HrContract::getStartDate).orderByDesc(HrContract::getCreateTime);
        IPage<HrContract> result = contractMapper.selectPage(p, wrapper);
        return new PageResult<>(
                contractConverter.toVOList(result.getRecords()),
                result.getTotal(),
                result.getCurrent(),
                result.getSize());
    }

    @Override
    @Transactional
    public String create(ContractCreateRequest request) {
        HrEmployee employee = employeeMapper.selectById(request.getEmployeeId());
        if (employee == null) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "员工档案不存在");
        }
        validateContractNo(request.getContractNo(), null);
        HrContract contract = new HrContract();
        contract.setEmployeeId(request.getEmployeeId());
        contract.setContractNo(request.getContractNo().trim());
        contract.setContractType(request.getContractType());
        contract.setStartDate(request.getStartDate());
        contract.setEndDate(request.getEndDate());
        contract.setSignDate(request.getSignDate());
        contract.setFileId(request.getFileId());
        contract.setStatus(request.getStatus() == null ? ContractStatus.ACTIVE : request.getStatus());
        contract.setRenewalRemindDate(request.getRenewalRemindDate());
        contract.setRemark(request.getRemark());
        contractMapper.insert(contract);
        log.info("新建劳动合同 {}（员工 {}）", contract.getId(), contract.getEmployeeId());
        return String.valueOf(contract.getId());
    }

    @Override
    @Transactional
    public void update(Long id, ContractUpdateRequest request) {
        HrContract contract = contractMapper.selectById(id);
        if (contract == null) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "劳动合同不存在");
        }
        if (request.getContractNo() != null
                && !request.getContractNo().trim().equals(contract.getContractNo())) {
            validateContractNo(request.getContractNo(), id);
            contract.setContractNo(request.getContractNo().trim());
        }
        if (request.getContractType() != null) {
            contract.setContractType(request.getContractType());
        }
        if (request.getStartDate() != null) {
            contract.setStartDate(request.getStartDate());
        }
        if (request.getEndDate() != null) {
            contract.setEndDate(request.getEndDate());
        }
        if (request.getSignDate() != null) {
            contract.setSignDate(request.getSignDate());
        }
        if (request.getFileId() != null) {
            contract.setFileId(request.getFileId());
        }
        if (request.getStatus() != null) {
            contract.setStatus(request.getStatus());
        }
        if (request.getRenewalRemindDate() != null) {
            contract.setRenewalRemindDate(request.getRenewalRemindDate());
        }
        if (request.getRemark() != null) {
            contract.setRemark(request.getRemark());
        }
        contractMapper.updateById(contract);
        log.info("更新劳动合同 {}", contract.getId());
    }

    @Override
    public List<ContractVO> expiring(int days) {
        int span = Math.max(days, 0);
        LocalDate today = LocalDate.now();
        LocalDate deadline = today.plusDays(span);
        List<HrContract> contracts =
                contractMapper.selectList(
                        new LambdaQueryWrapper<HrContract>()
                                .eq(HrContract::getStatus, ContractStatus.ACTIVE)
                                .isNotNull(HrContract::getEndDate)
                                .ge(HrContract::getEndDate, today)
                                .le(HrContract::getEndDate, deadline)
                                .orderByAsc(HrContract::getEndDate));
        return contractConverter.toVOList(contracts);
    }

    private void validateContractNo(String contractNo, Long excludeId) {
        Long count =
                contractMapper.selectCount(
                        new LambdaQueryWrapper<HrContract>()
                                .eq(HrContract::getContractNo, contractNo.trim())
                                .ne(excludeId != null, HrContract::getId, excludeId));
        if (count != null && count > 0) {
            throw BusinessException.of(
                    ErrorCode.BIZ_DUPLICATE_CODE, "合同编号已存在：" + contractNo.trim());
        }
    }
}
