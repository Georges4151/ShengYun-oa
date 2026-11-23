package com.ceams.module.hr.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ceams.common.enums.ContractApprovalStatus;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.common.response.PageResult;
import com.ceams.module.bpm.dto.InstanceStartRequest;
import com.ceams.module.bpm.enums.BpmBizType;
import com.ceams.module.bpm.service.BpmInstanceService;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 劳动合同服务实现。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HrContractServiceImpl implements HrContractService {

    /** 劳动合同审批默认流程模板编码（见 V54 迁移）。 */
    private static final String APPROVAL_TEMPLATE_CODE = "hr-contract-approval";

    private final HrContractMapper contractMapper;
    private final HrEmployeeMapper employeeMapper;
    private final HrContractConverter contractConverter;
    private final BpmInstanceService bpmInstanceService;

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
        contract.setFilePath(request.getFilePath());
        contract.setFileName(request.getFileName());
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
        if (StringUtils.isNotBlank(request.getFilePath())) {
            contract.setFilePath(request.getFilePath());
        }
        if (StringUtils.isNotBlank(request.getFileName())) {
            contract.setFileName(request.getFileName());
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
    @Transactional(rollbackFor = Exception.class)
    public String submitApproval(Long id) {
        HrContract contract = contractMapper.selectById(id);
        if (contract == null) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "劳动合同不存在");
        }
        ContractApprovalStatus status =
                contract.getApprovalStatus() == null
                        ? ContractApprovalStatus.NONE
                        : contract.getApprovalStatus();
        // 仅未提交或已驳回可再次发起，审批中/已通过重复提交直接报错，不新建流程实例
        if (status != ContractApprovalStatus.NONE && status != ContractApprovalStatus.REJECTED) {
            throw BusinessException.of(ErrorCode.BIZ_STATE_CONFLICT, "该合同已提交审批或已通过");
        }

        HrEmployee employee =
                contract.getEmployeeId() == null
                        ? null
                        : employeeMapper.selectById(contract.getEmployeeId());
        InstanceStartRequest request = new InstanceStartRequest();
        request.setTemplateCode(APPROVAL_TEMPLATE_CODE);
        request.setBizType(BpmBizType.HR_CONTRACT.name());
        request.setBizId(contract.getId());
        Map<String, String> variables = new LinkedHashMap<>();
        variables.put("contractId", String.valueOf(contract.getId()));
        variables.put("contractNo", nullToEmpty(contract.getContractNo()));
        variables.put("employeeId", nullToEmpty(contract.getEmployeeId()));
        variables.put("employeeName", employee == null ? "" : nullToEmpty(employee.getName()));
        variables.put(
                "contractType",
                contract.getContractType() == null ? "" : contract.getContractType().name());
        variables.put("startDate", nullToEmpty(contract.getStartDate()));
        variables.put("endDate", nullToEmpty(contract.getEndDate()));
        request.setVariables(variables);

        String instanceId = bpmInstanceService.start(request);
        // 条件更新兜底并发重复提交：只有仍处于未提交/已驳回时才能写回，事务回滚会同时撤销实例
        HrContract patch = new HrContract();
        patch.setApprovalStatus(ContractApprovalStatus.PENDING);
        patch.setBpmInstanceId(Long.valueOf(instanceId));
        int updated =
                contractMapper.update(
                        patch,
                        new LambdaUpdateWrapper<HrContract>()
                                .eq(HrContract::getId, id)
                                .in(
                                        HrContract::getApprovalStatus,
                                        ContractApprovalStatus.NONE,
                                        ContractApprovalStatus.REJECTED));
        if (updated == 0) {
            throw BusinessException.of(ErrorCode.BIZ_STATE_CONFLICT, "该合同已提交审批或已通过");
        }
        log.info("劳动合同 {} 已提交并发起 BPM 实例 {}", id, instanceId);
        return instanceId;
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

    /** 流程变量统一字符串：空值写空串，避免 Map 中出现 null。 */
    private String nullToEmpty(Object value) {
        return value == null ? "" : value.toString();
    }
}
