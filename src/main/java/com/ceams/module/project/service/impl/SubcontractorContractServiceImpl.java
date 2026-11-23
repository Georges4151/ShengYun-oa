package com.ceams.module.project.service.impl;

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
import com.ceams.module.hr.support.SimpleXlsxWriter;
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
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 分包合同服务实现。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubcontractorContractServiceImpl implements SubcontractorContractService {

    /** 分包合同审批默认流程模板编码（见 V54 迁移）。 */
    private static final String APPROVAL_TEMPLATE_CODE = "subcontractor-contract-approval";

    /** 导出列顺序。 */
    private static final List<String> EXPORT_HEADERS =
            List.of(
                    "合同名称",
                    "合同编号",
                    "分包商",
                    "项目",
                    "合同金额",
                    "已付金额",
                    "状态",
                    "开始日期",
                    "结束日期",
                    "附件文件名",
                    "创建时间");

    private final SubcontractorContractMapper subcontractorContractMapper;
    private final SubcontractorMapper subcontractorMapper;
    private final ProjectMapper projectMapper;
    private final BudgetMapper budgetMapper;
    private final SettlementProofMapper settlementProofMapper;
    private final SubcontractorContractConverter subcontractorContractConverter;
    private final BpmInstanceService bpmInstanceService;

    @Override
    public PageResult<SubcontractorContractVO> page(
            long page, long size, Long subcontractorId, Long projectId) {
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
        List<SubcontractorContractVO> records =
                subcontractorContractConverter.toVOList(result.getRecords());
        fillNames(records);
        return new PageResult<>(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public SubcontractorContractVO detail(Long id) {
        SubcontractorContractVO vo = subcontractorContractConverter.toVO(requireContract(id));
        fillNames(List.of(vo));
        return vo;
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
        contract.setFilePath(request.getFilePath());
        contract.setFileName(request.getFileName());
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
        if (StringUtils.isNotBlank(request.getFilePath())) {
            contract.setFilePath(request.getFilePath());
        }
        if (StringUtils.isNotBlank(request.getFileName())) {
            contract.setFileName(request.getFileName());
        }
        subcontractorContractMapper.updateById(contract);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        requireContract(id);
        Long budgetRefs =
                budgetMapper.selectCount(
                        new LambdaQueryWrapper<Budget>().eq(Budget::getContractId, id));
        if (budgetRefs != null && budgetRefs > 0) {
            throw BusinessException.of(ErrorCode.BIZ_REFERENCED, "分包合同存在关联预算，无法删除");
        }
        Long proofRefs =
                settlementProofMapper.selectCount(
                        new LambdaQueryWrapper<SettlementProof>()
                                .eq(SettlementProof::getContractId, id));
        if (proofRefs != null && proofRefs > 0) {
            throw BusinessException.of(ErrorCode.BIZ_REFERENCED, "分包合同存在关联结算凭证，无法删除");
        }
        subcontractorContractMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String submitApproval(Long id) {
        SubcontractorContract contract = subcontractorContractMapper.selectById(id);
        if (contract == null) {
            throw BusinessException.of(ErrorCode.BIZ_NOT_FOUND, "分包合同不存在");
        }
        ContractApprovalStatus status =
                contract.getApprovalStatus() == null
                        ? ContractApprovalStatus.NONE
                        : contract.getApprovalStatus();
        // 仅未提交或已驳回可再次发起，审批中/已通过重复提交直接报错，不新建流程实例
        if (status != ContractApprovalStatus.NONE && status != ContractApprovalStatus.REJECTED) {
            throw BusinessException.of(ErrorCode.BIZ_STATE_CONFLICT, "该合同已提交审批或已通过");
        }

        Subcontractor subcontractor =
                contract.getSubcontractorId() == null
                        ? null
                        : subcontractorMapper.selectById(contract.getSubcontractorId());
        Project project =
                contract.getProjectId() == null
                        ? null
                        : projectMapper.selectById(contract.getProjectId());
        InstanceStartRequest request = new InstanceStartRequest();
        request.setTemplateCode(APPROVAL_TEMPLATE_CODE);
        request.setBizType(BpmBizType.SUBCONTRACT_CONTRACT.name());
        request.setBizId(contract.getId());
        Map<String, String> variables = new LinkedHashMap<>();
        variables.put("contractId", String.valueOf(contract.getId()));
        variables.put("contractNumber", nullToEmpty(contract.getContractNumber()));
        variables.put("contractName", nullToEmpty(contract.getContractName()));
        variables.put("subcontractorId", nullToEmpty(contract.getSubcontractorId()));
        variables.put(
                "subcontractorName",
                subcontractor == null ? "" : nullToEmpty(subcontractor.getName()));
        variables.put("projectId", nullToEmpty(contract.getProjectId()));
        variables.put("projectName", project == null ? "" : nullToEmpty(project.getProjectName()));
        variables.put(
                "contractAmount",
                contract.getContractAmount() == null
                        ? "0"
                        : contract.getContractAmount().toPlainString());
        variables.put("startDate", nullToEmpty(contract.getStartDate()));
        variables.put("endDate", nullToEmpty(contract.getEndDate()));
        request.setVariables(variables);

        String instanceId = bpmInstanceService.start(request);
        // 条件更新兜底并发重复提交：只有仍处于未提交/已驳回时才能写回，事务回滚会同时撤销实例
        SubcontractorContract patch = new SubcontractorContract();
        // paidAmount 字段有默认值 ZERO，条件更新时必须显式置空，避免把已付金额覆盖为 0
        patch.setPaidAmount(null);
        patch.setApprovalStatus(ContractApprovalStatus.PENDING);
        patch.setBpmInstanceId(Long.valueOf(instanceId));
        int updated =
                subcontractorContractMapper.update(
                        patch,
                        new LambdaUpdateWrapper<SubcontractorContract>()
                                .eq(SubcontractorContract::getId, id)
                                .in(
                                        SubcontractorContract::getApprovalStatus,
                                        ContractApprovalStatus.NONE,
                                        ContractApprovalStatus.REJECTED));
        if (updated == 0) {
            throw BusinessException.of(ErrorCode.BIZ_STATE_CONFLICT, "该合同已提交审批或已通过");
        }
        log.info("分包合同 {} 已提交并发起 BPM 实例 {}", id, instanceId);
        return instanceId;
    }

    @Override
    public void export(OutputStream out, Long subcontractorId, Long projectId) {
        LambdaQueryWrapper<SubcontractorContract> wrapper = new LambdaQueryWrapper<>();
        if (subcontractorId != null) {
            wrapper.eq(SubcontractorContract::getSubcontractorId, subcontractorId);
        }
        if (projectId != null) {
            wrapper.eq(SubcontractorContract::getProjectId, projectId);
        }
        wrapper.orderByDesc(SubcontractorContract::getCreateTime);
        List<SubcontractorContract> contracts = subcontractorContractMapper.selectList(wrapper);
        Set<Long> subcontractorIds =
                contracts.stream()
                        .map(SubcontractorContract::getSubcontractorId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
        Set<Long> projectIds =
                contracts.stream()
                        .map(SubcontractorContract::getProjectId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
        Map<Long, String> subcontractorNames = loadSubcontractorNames(subcontractorIds);
        Map<Long, String> projectNames = loadProjectNames(projectIds);
        List<List<Object>> rows = new ArrayList<>(contracts.size());
        for (SubcontractorContract contract : contracts) {
            List<Object> row = new ArrayList<>(EXPORT_HEADERS.size());
            row.add(contract.getContractName());
            row.add(contract.getContractNumber());
            row.add(
                    nameOrId(
                            subcontractorNames.get(contract.getSubcontractorId()),
                            contract.getSubcontractorId()));
            row.add(nameOrId(projectNames.get(contract.getProjectId()), contract.getProjectId()));
            row.add(contract.getContractAmount());
            row.add(contract.getPaidAmount());
            row.add(contract.getStatus() == null ? null : contract.getStatus().name());
            row.add(dateText(contract.getStartDate()));
            row.add(dateText(contract.getEndDate()));
            row.add(contract.getFileName());
            row.add(contract.getCreateTime() == null ? null : contract.getCreateTime().toString());
            rows.add(row);
        }
        SimpleXlsxWriter.write(out, "分包合同", EXPORT_HEADERS, rows);
        log.info(
                "分包合同导出完成：分包商 {}，项目 {}，条数 {}", subcontractorId, projectId, rows.size());
    }

    /**
     * 批量填充分包商名称与项目名称（展示用）。
     *
     * <p>先收集本批记录中的非空 ID，再各用一次批量查询换取名称，避免逐条查询造成 N+1；
     * 查不到（含记录已删除）时保持名称字段为 null。
     */
    private void fillNames(List<SubcontractorContractVO> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        Set<Long> subcontractorIds =
                records.stream()
                        .map(SubcontractorContractVO::getSubcontractorId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
        Set<Long> projectIds =
                records.stream()
                        .map(SubcontractorContractVO::getProjectId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
        Map<Long, String> subcontractorNames = loadSubcontractorNames(subcontractorIds);
        Map<Long, String> projectNames = loadProjectNames(projectIds);
        for (SubcontractorContractVO vo : records) {
            if (vo.getSubcontractorId() != null) {
                vo.setSubcontractorName(subcontractorNames.get(vo.getSubcontractorId()));
            }
            if (vo.getProjectId() != null) {
                vo.setProjectName(projectNames.get(vo.getProjectId()));
            }
        }
    }

    private Map<Long, String> loadSubcontractorNames(Collection<Long> ids) {
        Map<Long, String> names = new HashMap<>();
        if (ids == null || ids.isEmpty()) {
            return names;
        }
        for (Subcontractor subcontractor : subcontractorMapper.selectBatchIds(ids)) {
            names.put(subcontractor.getId(), subcontractor.getName());
        }
        return names;
    }

    private Map<Long, String> loadProjectNames(Collection<Long> ids) {
        Map<Long, String> names = new HashMap<>();
        if (ids == null || ids.isEmpty()) {
            return names;
        }
        for (Project project : projectMapper.selectBatchIds(ids)) {
            names.put(project.getId(), project.getProjectName());
        }
        return names;
    }

    /** 名称缺失时回退写 ID 文本。 */
    private String nameOrId(String name, Long id) {
        return StringUtils.isNotBlank(name) ? name : idText(id);
    }

    /** ID 按字符串写出，避免 Excel 数值单元格丢失雪花 ID 精度。 */
    private String idText(Long id) {
        return id == null ? null : String.valueOf(id);
    }

    /** 日期写出为 yyyy-MM-dd 文本。 */
    private String dateText(LocalDateTime date) {
        return date == null ? null : date.toLocalDate().toString();
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
        LambdaQueryWrapper<SubcontractorContract> wrapper =
                new LambdaQueryWrapper<SubcontractorContract>()
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

    /** 流程变量统一字符串：空值写空串，避免 Map 中出现 null。 */
    private String nullToEmpty(Object value) {
        return value == null ? "" : value.toString();
    }
}
